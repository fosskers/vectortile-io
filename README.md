VectorTile IO
=============

This demo writes and reads a simple layer of 10x10 VectorTiles in a few ways:

1. to and from S3 via `S3Layer{Reader,Writer}` with Avro encoding
2. to the filesystem as readily usable `.mvt` files
3. to S3 as `.mvt` files

In order for this demo to compile, you will need to have run:

```console
> ./sbt -211 "project vectortile" publish-local
```

on the current GeoTrellis master branch (unreleased `1.0.0` as of 2016 October 7).

### Usage

`sbt` is your best bet:

```console
> sbt run COMMAND
```

Where `COMMAND` is one of:

- `s3Avro`
- `s3Mvt`
- `fsMvt`

If interacting with S3, ensure that you've set `bucket`, `keyPrefix`, and
`layerId` to things you have access to on S3:

```scala
val bucket = "azavea-datahub"
val keyPrefix = "catalog"
...
val layerId = LayerId("vt-io-test", 1)
```
