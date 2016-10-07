package geotrellis.vectortile

import geotrellis.raster.TileLayout
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index.{ KeyIndex, ZCurveKeyIndexMethod }
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.s3._
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.spark.util.SparkUtils
import geotrellis.util.GetComponent
import geotrellis.vector.Extent
import geotrellis.vectortile.protobuf.ProtobufTile
import geotrellis.vectortile.spark.Implicits._

import java.nio.file.{ Files, Paths }
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.DefaultJsonProtocol._
import spray.json._

// --- //

/** Minimalist Layer-level metadata. */
case class Megadata[K: JsonFormat](layout: LayoutDefinition, bounds: KeyBounds[K])

object Megadata {
  /* A Lens into the key bounds */
  implicit def megadataGet[K]: GetComponent[Megadata[K], Bounds[K]] =
    GetComponent(_.bounds)

  /* Json Conversion */
  implicit def megadataFormat[K: JsonFormat] = jsonFormat2(Megadata[K])
}

object IO extends App {

  override def main(args: Array[String]): Unit = {
    implicit val sc: SparkContext =
      SparkUtils.createLocalSparkContext("local[*]", "VectorTiles IO Test")

    /* Source tile. It has 15 internal Layers and 1207 total geometries.
     * In its `.mvt` (protobuf) form, it is 40kb.
     */
    val bytes: Array[Byte] = Files.readAllBytes(Paths.get("roads.mvt"))

    /* RDD Setup */
    val layout = LayoutDefinition(Extent(0, 0, 40960, 40960), TileLayout(10, 10, 4096, 4096))
    val bounds = KeyBounds(SpatialKey(0, 0), SpatialKey(9, 9))
    val metadata = Megadata(layout, bounds)

    val pairs: Seq[(SpatialKey, VectorTile)] = for {
      x <- 0 to 9
      y <- 0 to 9
    } yield {
      val key = SpatialKey(x, y)
      val tile: VectorTile = ProtobufTile.fromBytes(bytes, layout.mapTransform(key))

      key -> tile
    }

    /* A layer of 100 of the same tile arranged in a grid */
    val rdd0: RDD[(SpatialKey, VectorTile)] with Metadata[Megadata[SpatialKey]] =
      ContextRDD(sc.parallelize(pairs), metadata)

    args match {
      case Array("s3Avro") => { /* S3 IO */
        /* S3 IO Config */
        val bucket = "azavea-datahub"
        val keyPrefix = "catalog"
        val store = new S3AttributeStore(bucket, keyPrefix)
        val writer = new S3LayerWriter(store, bucket, keyPrefix)
        val reader = new S3LayerReader(store)
        val index: KeyIndex[SpatialKey] = ZCurveKeyIndexMethod.createIndex(bounds)
        val layerId = LayerId("vt-io-test5", 1)

        println("Writing to S3...")

        /* Write to S3. This requires a good handful of implicits to be in scope. */
        writer.write[SpatialKey, VectorTile, Megadata[SpatialKey]](layerId, rdd0, index)

        println("Write complete.")

        println("Reading from S3...")

        /* Read from S3 */
        val rdd1: RDD[(SpatialKey, VectorTile)] with Metadata[Megadata[SpatialKey]] =
          reader.read[SpatialKey, VectorTile, Megadata[SpatialKey]](layerId)

        val same: Boolean = rdd0.count == rdd1.count

        println(s"Done. Same RDD? --> ${same}")
      }
      case Array("fsMvt") => {
        println("Saving VectorTiles to filesystem...")

        val rdd1: RDD[(SpatialKey, Array[Byte])] = rdd0.mapValues({
          case v: ProtobufTile => v.toBytes
          case _ => throw new IllegalArgumentException("Expected a ProtobufTile")
        })

        /* Setup for saving to the file system */
        val template = s"/home/colin/vt-cache/catalog/{name}/{z}/{x}/{y}.mvt"
        val id = LayerId("sample", 1)

        val keyToPath: SpatialKey => String =
          SaveToHadoop.spatialKeyToPath(id, template)

        val wrote: Long = rdd1.saveToHadoop(keyToPath)

        println(s"Wrote ${wrote} VectorTiles")
      }
      case Array("s3Mvt") => {
        println("Saving VectorTiles to S3 as .mvt files...")

        val rdd1: RDD[(SpatialKey, Array[Byte])] = rdd0.mapValues({
          case v: ProtobufTile => v.toBytes
          case _ => throw new IllegalArgumentException("Expected a ProtobufTile")
        })

        /* Setup for saving to S3 */
        val template = "s3://azavea-datahub/catalog/{name}/{z}/{x}/{y}.mvt"
        val id = LayerId("sample", 1)

        val keyToPath: SpatialKey => String =
          SaveToS3.spatialKeyToPath(id, template)

        rdd1.saveToS3(keyToPath)

        println("Wrote MVT files to S3.")

      }
      case _ => println("Usage: run {s3Avro|fsMvt}")
    }

    /* Safely shut down Spark */
    sc.stop()
  }
}
