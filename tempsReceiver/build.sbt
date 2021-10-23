name := "temp-receiver"

version := "1.0"

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.2.2",
  "com.typesafe.akka" %% "akka-stream" % "2.6.10",
  "com.typesafe.akka" %% "akka-actor-typed" % "2.6.10",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.2.2",
  // https://mongodb.github.io/mongo-scala-driver/2.9/getting-started/installation-guide/
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.9.0"
)
