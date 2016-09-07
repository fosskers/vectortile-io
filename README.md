VectorTile IO
=============

This demo writes and reads a simple layer of 10x10 VectorTiles to and from
S3. In order for it to compile, you will need to have run:

```console
> ./sbt -211 "project vectortile" publish-local
```

on the branch found in [this PR](https://github.com/geotrellis/geotrellis/pull/1622).

### Usage

Ensure that you've set `bucket`, `keyPrefix`, and `layerId` to things
you have access to on S3:

```scala
val bucket = "azavea-datahub"
val keyPrefix = "catalog"
...
val layerId = LayerId("vt-io-test", 1)
```

Then you can run the demo in `sbt` with:

```console
> sbt run
```
