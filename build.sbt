name := "ner-scala"
 
version := "1.0"
 
scalaVersion := "2.11.8"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test"

libraryDependencies += "com.generall" %% "sknn-scala" % "1.0-SNAPSHOT"
libraryDependencies += "com.generall" %% "ontology" % "1.0-SNAPSHOT"


libraryDependencies += "ml.generall" %% "elastic-scala" % "1.0-SNAPSHOT"
libraryDependencies += "ml.generall" %% "sentence-chunker" % "1.0-SNAPSHOT"


libraryDependencies += "com.propensive" %% "rapture" % "2.0.0-M7"
libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.2.5"
libraryDependencies += "org.typelevel" %% "scalaz-outlaws" % "0.2"


resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/stew/snapshots"

resolvers += Resolver.mavenLocal
