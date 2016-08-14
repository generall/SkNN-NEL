package com.generall.ner

import org.scalatest.FunSuite

/**
  * Created by generall on 13.08.16.
  */
class TaggerTest extends FunSuite {


  test("taggSentance"){
    /*
      Training set:

      Andrei Tarkovsky filmed Stalker_(1979_film)

      Lenin used Russian_cruiser_Aurora

      Validation set:

      James Cameron made Titanic(Ship or Film)

      Edward Smith ruled Titanic(Ship or Film)

     */


    val tarkovsky = new OntologyElement("http://dbpedia.org/page/Andrei_Tarkovsky")
    val stalker = new OntologyElement("http://dbpedia.org/page/Stalker_(1979_film)")

    val lenin = new OntologyElement("http://dbpedia.org/page/Vladimir_Lenin")
    val aurora = new OntologyElement("http://dbpedia.org/page/Russian_cruiser_Aurora")

    val cameron = new OntologyElement("http://dbpedia.org/page/James_Cameron")
    val smith = new OntologyElement("http://dbpedia.org/page/Edward_Smith_(sea_captain)")

    val titanic = new MultiElement[OntologyElement]

    val titanic_film = new OntologyElement("http://dbpedia.org/page/Titanic_(1997_film)")
    val titanic_ship = new OntologyElement("http://dbpedia.org/page/RMS_Titanic")

    titanic.addElement(titanic_film)
    titanic.addElement(titanic_ship)

    val training = List(
      List(tarkovsky, stalker),
      List(lenin, aurora)
    )

    val test1 = List(

    )

//    val model = new Model[BaseElement, SkNNNode[BaseElement]]((label) => {
//      new SkNNNodeImpl[BaseElement, PlainAverageStorage[BaseElement]](label, 1)( () => {
//        new PlainAverageStorage[BaseElement]((x, y) => Measures.weightedJaccardDisatnce)
//      })
//    })

  }

}
