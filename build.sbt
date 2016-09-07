name := """vectortile-io"""

version := "1.0"

scalaVersion := "2.11.8"

// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
  "com.azavea.geotrellis" %% "geotrellis-spark"      % "1.0.0-SNAPSHOT",
  "com.azavea.geotrellis" %% "geotrellis-vector"     % "1.0.0-SNAPSHOT",
  "com.azavea.geotrellis" %% "geotrellis-vectortile" % "1.0.0-SNAPSHOT",
  "com.azavea.geotrellis" %% "geotrellis-s3"         % "1.0.0-SNAPSHOT",
  "org.apache.spark"      %% "spark-core"            % "1.6.2"
)

// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.11"
