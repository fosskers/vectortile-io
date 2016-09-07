package geotrellis.vectortile

import geotrellis.raster.TileLayout
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index.{ KeyIndex, ZCurveKeyIndexMethod }
import geotrellis.spark.io.s3.{ S3AttributeStore, S3LayerReader, S3LayerWriter }
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.spark.util.SparkUtils
import geotrellis.util.GetComponent
import geotrellis.vector.Extent
import geotrellis.vectortile.protobuf.ProtobufTile
import geotrellis.vectortile.spark.Implicits._

import java.nio.file.{ Files, Paths }
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._
import spray.json.DefaultJsonProtocol._

// --- //

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

    /* RDD Setup */
    val layout = LayoutDefinition(Extent(0, 0, 4096, 4096), TileLayout(1, 1, 4096, 4096))
    val bytes: Array[Byte] = Files.readAllBytes(Paths.get("roads.mvt"))
    val key = SpatialKey(0, 0)
    val tile = ProtobufTile.fromBytes(bytes, layout.mapTransform(key))
    val bounds = KeyBounds(key, key)
    val metadata = Megadata(layout, bounds)

    /* A singleton RDD with one Tile */
    val rdd0: RDD[(SpatialKey, VectorTile)] with Metadata[Megadata[SpatialKey]] =
      ContextRDD(sc.parallelize(Seq(key -> tile)), metadata)

    /* S3 IO Config */
    val bucket = "azavea-datahub"
    val keyPrefix = "catalog"
    val store = new S3AttributeStore(bucket, keyPrefix)
    val writer = new S3LayerWriter(store, bucket, keyPrefix)
    val reader = new S3LayerReader(store)
    val index: KeyIndex[SpatialKey] = ZCurveKeyIndexMethod.createIndex(bounds)
    val layerId = LayerId("vt-io-test4", 1)

    println("Writing to S3...")

    /* Write to S3 */
    writer.write[SpatialKey, VectorTile, Megadata[SpatialKey]](layerId, rdd0, index)

    println("Write complete.")

    println("Reading from S3...")

    /* Read from S3 */
    val rdd1: RDD[(SpatialKey, VectorTile)] with Metadata[Megadata[SpatialKey]] =
      reader.read[SpatialKey, VectorTile, Megadata[SpatialKey]](layerId)

    val same: Boolean = rdd0.first()._2.layers.keySet == rdd1.first()._2.layers.keySet

    println(s"Done. Same RDD? --> ${same}")

    sc.stop()
  }
}
