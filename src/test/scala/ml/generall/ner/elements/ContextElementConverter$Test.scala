package ml.generall.ner.elements

import ml.generall.ner.ElementMeasures
import org.scalatest.FunSuite

/**
  * Created by generall on 15.08.16.
  */
class ContextElementConverter$Test extends FunSuite {

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

  val ruled = new POSElement(POSTag("ruled", "verb"))
  val command = new POSElement(POSTag("command", "verb"))
  val made = new POSElement(POSTag("made", "verb"))
  val filmed = new POSElement(POSTag("filmed", "verb"))

  def sumRes(list1: List[ContextElement], list2: List[ContextElement] ): Double = {
    assert(list1.size == list2.size)
    list1.zip(list2).foldLeft(0.0)((sum, pair)=> sum + ElementMeasures.baseElementDistance(pair._1, pair._2))
  }

  test("testConvert") {

    val list1 = List(
      smith,
      ruled,
      titanic
    )

    val list2 = List(
      quayle,
      command,
      mona
    )

    val list3 = List(
      cameron,
      filmed,
      titanic
    )

    val list4 = List(
      tarkovsky,
      made,
      stalker
    )

    val res1 = ContextElementConverter.convert(list1, 3)
    val res2 = ContextElementConverter.convert(list2, 3)
    val res3 = ContextElementConverter.convert(list3, 3)
    val res4 = ContextElementConverter.convert(list4, 3)

    assert(res1.size == 3)

    val head1 = res1.head
    val head2 = res2.head

    assert(head1.label == smith.label)
    assert(head2.label == quayle.label)

    println(ElementMeasures.baseElementDistance(head1, head2))

    println(ElementMeasures.baseElementDistance(EmptyWeightedElement, EmptyWeightedElement))
    println(ElementMeasures.baseElementDistance(quayle, smith))
    println(ElementMeasures.baseElementDistance(ruled, command))

    assert(ElementMeasures.baseElementDistance(head1, head2)
      - (ElementMeasures.baseElementDistance(quayle, smith) + ElementMeasures.baseElementDistance(ruled, command)) < 0.0001)

    println("Cameron vs tarkosky: " ++ sumRes(res3, res4).toString)
    println("Cameron vs quayle: " ++ sumRes(res3, res2).toString)
    println("smith vs tarkosky: " ++ sumRes(res1, res4).toString)
    println("smith vs quayle: " ++ sumRes(res1, res2).toString)

  }

  test("testMakeVariants"){
    val contextConxept = new ContextElement(List(NullElement, titanic), titanic)
    val variants = ContextElementConverter.makeVariants(contextConxept)

    assert(titanic.subElements.size == 2)
    assert(variants.size == 2)
    variants.foreach(x => println(x.mainElement.label))

    assert(
      (variants(0).mainElement == titanic_film && variants(1).mainElement == titanic_ship) ||
      (variants(0).mainElement == titanic_ship && variants(1).mainElement == titanic_film)
    )

  }

}
