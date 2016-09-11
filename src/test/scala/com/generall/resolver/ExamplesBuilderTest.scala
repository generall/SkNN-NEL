package com.generall.resolver

import ml.generall.elastic.{Chunk, ConceptVariant}
import org.scalatest.{BeforeAndAfterEach, FunSuite}

/**
  * Created by generall on 27.08.16.
  */
class ExamplesBuilderTest extends FunSuite with BeforeAndAfterEach {

  var builder = new ExamplesBuilder()

  override def beforeEach() {
  }

  test("testMakeTrain") {

    val data = List(
      ("NP",
        List(
          ("Moscow", ("NNP", "B-NP"))
        )
        ),
      ("VP",
        List(
          ("is", ("VB", "B-VP"))
        )
        ),
      ("NP",
        List(
          ("soviet", ("NN", "B-NP")),
          ("city", ("NN", "I-NP"))
        )
        )
    )
    val trainList = Builder.makeTrain(data)
    trainList.foreach(trainObj => {
      trainObj.print()
    })

    assert(trainList(0).concepts.nonEmpty)
    assert(trainList(1).concepts.isEmpty)
    assert(trainList(2).concepts.nonEmpty)
  }

  test("testBuild") {
    val res = builder.build("http://en.wikipedia.org/wiki/RMS_Titanic")
    res.foreach(seq => {
      seq.foreach(_.print())
      println()
      println(" ================================= ")
      println()
    })
  }


  test("testBuildWithAnnotations") {

    val sent = "I am good at science and math."

    val sent2 = "Edward Smith is a captain of Titanic"

    val conceptVariant = List(ConceptVariant(
      concept = "url:science",
      count = 1,
      avgScore = 1.0,
      avgNorm = 1.0,
      avgSoftMax = 1.0,
      maxScore = 1.0,
      minScore = 1.0
    ))

    val conceptVariant1 = List(ConceptVariant(
      concept = "url:Edward_Smith",
      count = 1,
      avgScore = 1.0,
      avgNorm = 1.0,
      avgSoftMax = 1.0,
      maxScore = 1.0,
      minScore = 1.0
    ))

    val conceptVariant2 = List(ConceptVariant(
      concept = "url:RMS_Titanic",
      count = 1,
      avgScore = 1.0,
      avgNorm = 1.0,
      avgSoftMax = 1.0,
      maxScore = 1.0,
      minScore = 1.0
    ), ConceptVariant(
      concept = "url:Titanic_(film)",
      count = 1,
      avgScore = 1.0,
      avgNorm = 1.0,
      avgSoftMax = 1.0,
      maxScore = 1.0,
      minScore = 1.0
    ))


    val annotations = List(ConceptsAnnotation(13, 20, conceptVariant))

    val annotations1 = List(ConceptsAnnotation(0, 12, conceptVariant1), ConceptsAnnotation(29, 36, conceptVariant2))

    builder.buildFromAnnotations(sent, annotations).foreach(_.print())

    builder.buildFromAnnotations(sent2, annotations1).foreach(_.print())

  }


  test("testBuildFromMention"){
    val firstChunk = Chunk("I am good at")
    val middleChunk = Chunk("science", "url:Science")
    val lastChunk = Chunk("and math")

    val conceptVariant = List(ConceptVariant(
      concept = "url:science",
      count = 1,
      avgScore = 1.0,
      avgNorm = 1.0,
      avgSoftMax = 1.0,
      maxScore = 1.0,
      minScore = 1.0
    ))

    builder.buildFromMention(firstChunk, middleChunk, lastChunk, conceptVariant).foreach(_.print())


  }

}
