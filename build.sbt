name := "ner-scala"
 
organization := "ml.generall"

version := "1.0-SNAPSHOT"
 
scalaVersion := "2.11.8"

//conflictManager := ConflictManager.strict

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test"

libraryDependencies += "ml.generall" %% "sknn-scala" % "1.0-SNAPSHOT"
libraryDependencies += "ml.generall" %% "ontology" % "1.0-SNAPSHOT"


libraryDependencies += "ml.generall" %% "elastic-scala" % "1.0-SNAPSHOT"
libraryDependencies += "ml.generall" %% "sentence-chunker" % "1.0-SNAPSHOT"


libraryDependencies += "com.propensive" %% "rapture" % "2.0.0-M7"
libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.2.5"
libraryDependencies += "org.typelevel" %% "scalaz-outlaws" % "0.2"


/*
 * Needs for CoreNLP
 */
libraryDependencies += "com.fasterxml" % "aalto-xml" % "1.0.0"


resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/stew/snapshots"

resolvers += Resolver.mavenLocal
