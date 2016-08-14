package com.generall.ner

import com.generall.sknn.SkNN
import com.generall.sknn.model.storage.PlainAverageStorage
import com.generall.sknn.model.{SkNNNodeImpl, SkNNNode, Model}
import com.generall.sknn.model.storage.elements.BaseElement
import org.scalatest.FunSuite

/**
  * Created by generall on 13.08.16.
  */
class TaggerTest extends FunSuite {


  test("taggSentance"){
    /*
      Training set:

      Andrei Tarkovsky filmed Stalker
      Edward Quayle command SS Mona

      Test set:

      James Cameron made Titanic(Ship or Film)
      Edward Smith ruled Titanic(Ship or Film)

     */

    val tarkovsky = new OntologyElement("http://dbpedia.org/resource/Andrei_Tarkovsky")
    val stalker = new OntologyElement("http://dbpedia.org/resource/Stalker_(1979_film)")

    val quayle = new OntologyElement("http://dbpedia.org/resource/Edward_Quayle_(sea_captain)")
    val mona = new OntologyElement("http://dbpedia.org/resource/SS_Mona_(1832)")

    val cameron = new OntologyElement("http://dbpedia.org/resource/James_Cameron")
    val smith = new OntologyElement("http://dbpedia.org/resource/Edward_Smith_(sea_captain)")

    val titanic = new MultiElement[OntologyElement]

    val titanic_film = new OntologyElement("http://dbpedia.org/resource/Titanic_(1997_film)")
    val titanic_ship = new OntologyElement("http://dbpedia.org/resource/RMS_Titanic")

    titanic.addElement(titanic_film)
    titanic.addElement(titanic_ship)

    val training = List(
      List(tarkovsky, stalker),
      List(quayle, mona)
    )

    val test1 = List(cameron, titanic)

    val test2 = List(smith, titanic)


    val model = new Model[BaseElement, SkNNNode[BaseElement]]((label) => {
      new SkNNNodeImpl[BaseElement, PlainAverageStorage[BaseElement]](label, 1)( () => {
        new PlainAverageStorage[BaseElement](ElementMeasures.baseElementDistance)
      })
    })

    training.foreach(seq => model.processSequenceImpl(seq)(onto => List((onto.label, onto)) ))

    val sknn = new SkNN[BaseElement, SkNNNode[BaseElement]](model)

    val resFilm = sknn.tag(test1, 1)( (_, _) => true)
    val resShip = sknn.tag(test2, 1)( (_, _) => true)

    println("resFilm: ")
    resFilm.head._1.foreach(node => println(node.label))

    println("resShip: ")
    resShip.head._1.foreach(node => println(node.label))

  }

  def printDist(conceptUrl1: String, conceptUrl2: String)= {
    val concept1 = new OntologyElement(conceptUrl1)
    val concept2 = new OntologyElement(conceptUrl2)

    val dist = ElementMeasures.baseElementDistance(concept1, concept2)

    println(s"$conceptUrl1 vs $conceptUrl2 = $dist")
  }
  test("testConceptDistance") {

    printDist("http://dbpedia.org/resource/James_Cameron", "http://dbpedia.org/resource/Edward_Quayle_(sea_captain)")
    printDist("http://dbpedia.org/resource/RMS_Titanic", "http://dbpedia.org/resource/SS_Mona_(1832)")

    println("")

    printDist("http://dbpedia.org/resource/James_Cameron", "http://dbpedia.org/resource/Andrei_Tarkovsky")
    printDist("http://dbpedia.org/resource/Titanic_(1997_film)", "http://dbpedia.org/resource/Stalker_(1979_film)")

  }

}
