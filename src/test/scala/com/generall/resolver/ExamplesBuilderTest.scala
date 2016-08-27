package com.generall.resolver

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
      ( "NP",
        List(
          ("Moscow", ("NNP" , "B-NP"))
        )
      ),
      ( "VP",
        List(
          ("is", ("VB" , "B-VP"))
        )
      ),
      ( "NP",
        List(
          ("soviet", ("NN" , "B-NP")),
          ("city", ("NN" , "I-NP"))
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
    res.foreach(seq =>{
      seq.foreach(_.print())
      println()
      println(" ================================= ")
      println()
    })
  }

}
