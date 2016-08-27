name := "ner-scala"
 
version := "1.0"
 
scalaVersion := "2.11.8"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test"

libraryDependencies += "com.generall" %% "sknn-scala" % "1.0-SNAPSHOT"
libraryDependencies += "com.generall" %% "ontology" % "1.0-SNAPSHOT"


libraryDependencies += "ml.generall" %% "elastic-scala" % "1.0-SNAPSHOT"
libraryDependencies += "ml.generall" %% "sentence-chunker" % "1.0-SNAPSHOT"

resolvers += Resolver.mavenLocal
