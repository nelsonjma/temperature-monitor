name := "external-temp-collector"

version := "1.0"

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  "org.jsoup" % "jsoup" % "1.13.1", // https://jsoup.org/
  "com.typesafe.akka" %% "akka-actor" % "2.6.11",
  "org.scalaj" %% "scalaj-http" % "2.4.2" // https://github.com/scalaj/scalaj-http
)
