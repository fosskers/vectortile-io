name := """vectortile-io"""

version := "1.0.1"

scalaVersion := "2.11.8"

// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
  "org.locationtech.geotrellis" %% "geotrellis-spark"      % "1.0.0",
  "org.locationtech.geotrellis" %% "geotrellis-vector"     % "1.0.0",
  "org.locationtech.geotrellis" %% "geotrellis-vectortile" % "1.0.0",
  "org.locationtech.geotrellis" %% "geotrellis-s3"         % "1.0.0",
  "org.apache.spark"            %% "spark-core"            % "2.0.2"
)
