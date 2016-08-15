package com.generall.ner

import com.generall.ner.elements._
import com.generall.sknn.model.SkNNNodeImpl
import com.generall.sknn.model.storage.PlainAverageStorage
import com.generall.sknn.model.storage.elements.BaseElement
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter

/**
  * Created by generall on 15.08.16.
  */
class RecoverConcept$Test extends FunSuite with BeforeAndAfter {

  val quayle = new OntologyElement("http://dbpedia.org/resource/Edward_Quayle_(sea_captain)")
  val mona = new OntologyElement("http://dbpedia.org/resource/SS_Mona_(1832)")

  val smith = new OntologyElement("http://dbpedia.org/resource/Edward_Smith_(sea_captain)")
  val titanic = new MultiElement[OntologyElement]
  val titanic_film = new OntologyElement("http://dbpedia.org/resource/Titanic_(1997_film)")
  val titanic_ship = new OntologyElement("http://dbpedia.org/resource/RMS_Titanic")
  titanic.addElement(titanic_film)
  titanic.addElement(titanic_ship)

  val ruled = new POSElement(POSTag("ruled", "verb"))
  val command = new POSElement(POSTag("command", "verb"))

  test("testRecover") {

    val initNode = new SkNNNodeImpl[BaseElement, PlainAverageStorage[BaseElement]]("init", 1)(() => {
      new PlainAverageStorage[BaseElement](ElementMeasures.baseElementDistance)
    })

    val capNode = new SkNNNodeImpl[BaseElement, PlainAverageStorage[BaseElement]]("captainConcept", 1)(() => {
      new PlainAverageStorage[BaseElement](ElementMeasures.baseElementDistance)
    })

    val verbNode = new SkNNNodeImpl[BaseElement, PlainAverageStorage[BaseElement]]("VerbPOS", 1)(() => {
      new PlainAverageStorage[BaseElement](ElementMeasures.baseElementDistance)
    })

    val shipNode = new SkNNNodeImpl[BaseElement, PlainAverageStorage[BaseElement]]("shipConcept", 1)(() => {
      new PlainAverageStorage[BaseElement](ElementMeasures.baseElementDistance)
    })

    initNode.addLink(capNode)
    capNode.addLink(verbNode)
    verbNode.addLink(shipNode)

    initNode.addElement(quayle, "captainConcept")
    capNode.addElement(ruled, "VerbPOS")
    verbNode.addElement(mona, "shipConcept")

    val nodes = List(capNode, verbNode, shipNode)

    val elements = List(smith, command, titanic)

    val resolved = RecoverConcept.recover(elements, initNode, nodes) //.foreach(x => println(x.label))

    resolved.foreach(x => println(x.label))

    assert(resolved(2).label == titanic_ship.label)
  }

  test("testRecoverContext") {

    val initNode = new SkNNNodeImpl[BaseElement, PlainAverageStorage[BaseElement]]("init", 1)(() => {
      new PlainAverageStorage[BaseElement](ElementMeasures.baseElementDistance)
    })

    val capNode = new SkNNNodeImpl[BaseElement, PlainAverageStorage[BaseElement]]("captainConcept", 1)(() => {
      new PlainAverageStorage[BaseElement](ElementMeasures.baseElementDistance)
    })

    val verbNode = new SkNNNodeImpl[BaseElement, PlainAverageStorage[BaseElement]]("VerbPOS", 1)(() => {
      new PlainAverageStorage[BaseElement](ElementMeasures.baseElementDistance)
    })

    val shipNode = new SkNNNodeImpl[BaseElement, PlainAverageStorage[BaseElement]]("shipConcept", 1)(() => {
      new PlainAverageStorage[BaseElement](ElementMeasures.baseElementDistance)
    })

    initNode.addLink(capNode)
    capNode.addLink(verbNode)
    verbNode.addLink(shipNode)

    initNode.addElement(new ContextElement(List(NullElement, quayle, ruled), quayle), "captainConcept")
    capNode.addElement(new ContextElement(List(quayle, ruled, mona), ruled), "VerbPOS")
    verbNode.addElement(new ContextElement(List(ruled, mona, NullElement), mona), "shipConcept")

    val nodes = List(capNode, verbNode, shipNode)

    val elements = List(
      new ContextElement(List(NullElement, smith, command), smith),
      new ContextElement(List(smith, command, titanic), command),
      new ContextElement(List(command, titanic, NullElement), titanic)
    )

    val resolved = RecoverConcept.recover(elements, initNode, nodes) //.foreach(x => println(x.label))

    resolved.foreach(x => println(x.label))

    assert(resolved(2).label == titanic_ship.label)
  }

  test("testMatchElement") {

  }

}
