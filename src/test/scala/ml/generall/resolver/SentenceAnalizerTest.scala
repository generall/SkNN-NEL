package ml.generall.resolver

import ml.generall.common.StupidAssert
import ml.generall.ner.{ElementMeasures, RecoverConcept}
import ml.generall.ner.elements.{BagOfWordElement, ContextElement, ContextElementConverter, OntologyElement}
import ml.generall.sknn.SkNN
import ml.generall.sknn.model.storage.PlainAverageStorage
import ml.generall.sknn.model.{Model, SkNNNode, SkNNNodeImpl}
import ml.generall.sknn.model.storage.elements.BaseElement
import ml.generall.elastic.Chunk
import ml.generall.resolver.dto.ConceptVariant
import org.scalatest.FunSuite

import scala.collection.{immutable, mutable}

/**
  * Created by generall on 27.08.16.
  */
class SentenceAnalizerTest extends FunSuite {


  test("testGetTrainingSet") {
    val analizer = new SentenceAnalizer

    val ts = analizer.getTrainingSet(List(("", "http://en.wikipedia.org/wiki/Batman", "")))

    val categories = ts.flatMap(_.flatMap(x => x.flatMap {
      case x: OntologyElement => Some(x.features)
      case _ => None
    })).foldLeft(mutable.Map().withDefaultValue(0.0):  mutable.Map[String, Double])( (acc, x) => OntologyElement.joinFeatures(acc, x))

    categories.toList.sortBy(- _._2).foreach{case (k,v) => println(s"$k \t| $v")}
  }


  test("testAnalyse") {
    val analizer = new SentenceAnalizer

    analizer.analyse("I will go to London with Stalin on next weekend")
  }


  test("testTitanic") {

    val analizer = new SentenceAnalizer


    analizer.analyse("James Cameron made Titanic")
    analizer.analyse("On the next day Titanic struck Iceberg")

  }

  test("testTeresa") {

    val str = "When Mother Teresa received the Nobel Peace Price, she used the opportunity of her worldwide telecast speech in Oslo to declare abortion the greatest evil in the world and to launch a fiery call against population control."

    val analizer = new SentenceAnalizer

    analizer.analyse(str)

  }

  test("testFilter") {
    val str = "I will go to London"

    val analizer = new SentenceAnalizer


    val parseRes = analizer.parser.process(str)

    val groups = parseRes.zipWithIndex
      .groupBy({ case (record, idx) => (record.parseTag, "" /*record.ner*/ , record.groupId) })
      .toList
      .sortBy(x => x._2.head._2)
      .map(pair => pair._2.map(_._1))


    StupidAssert.assert(!Builder.filterChunk(groups.head))

    StupidAssert.assert(Builder.filterChunk(groups.last))

  }

  test("testPrepareSentence") {
    val str = "Yesterday the Titanic crashed into an iceberg"
    val analizer = new SentenceAnalizer

    val trains = analizer.prepareSentence(str)

    trains.foreach(_.print())
  }


  test("testNewParser") {


    val str = "Yesterday Titanic crashed into an iceberg"

    val exampleBuilder = new ExamplesBuilder

    val contextSize = 5

    val parser = LocalCoreNLP

    val parseRes = parser.process(str)

    parseRes.foreach(println)

    val groups = parseRes.zipWithIndex
      .groupBy({ case (record, idx) => (record.parseTag, "" /*record.ner*/ , record.groupId) })
      .toList
      .sortBy(x => x._2.head._2)
      .map(pair => (s"${pair._1._1}", pair._2.map(_._1))) // creation of state

    val objs = Builder.makeTrain(groups)

    objs.foreach(_.print())

    /**
      * All concepts with disambiguation
      */
    val conceptsToLearn: List[(String, String, String)] = SentenceAnalizer.getConceptsToLearn(objs, contextSize)

    println("conceptsToLearn: ")
    conceptsToLearn.foreach(println)

    val target = ContextElementConverter.convert(objs.map(SentenceAnalizer.toBagOfWordsElement), contextSize)


    val searchResults = List(
      exampleBuilder.buildFromMention(
        Chunk("1912 On its maiden voyage, the British ocean liner"),
        Chunk("RMS Titanic"),
        Chunk("struck an iceberg in the North Atlantic Ocean at about 11:40 pm ship'\ns time"),
        List(ConceptVariant("http://en.wikipedia.org/wiki/RMS_Titanic"))
      ),
      exampleBuilder.buildFromMention(
        Chunk("That moonless night of 14 April 1912 when the mighty"),
        Chunk("RMS Titanic"),
        Chunk("with 2,223 souls on board collided 37 seconds after the sighting of a partic\nular iceberg and the fateful annoucement by lookouts, \"Iceberg right ahead!\" There"),
        List(ConceptVariant("http://en.wikipedia.org/wiki/RMS_Titanic"))
      ),
      exampleBuilder.buildFromMention(
        Chunk("Southern Man Saturday, April 14, 2012 Shipwrecks and the Social Contract On this day a century ago the RMS"),
        Chunk("Titanic"),
        Chunk(", while on her maiden voyage from England to the United States, was mortally wounded when she grazed an iceberg that tore open her side."),
        List(ConceptVariant("http://en.wikipedia.org/wiki/Titanic_(1997_film)"))
      ),
      exampleBuilder.buildFromMention(
        Chunk("from them again. Tuld and Cohen intend to cash in on the crisis. They’re flipsides of the same tainted coin. ‘Margin Call’ is like ‘"),
        Chunk("Titanic"),
        Chunk("’ from the moment it hits the iceberg. Except, here, everyone survives. Even Eric Dale. They’re all on life-jackets out at sea, searching for land"),
        List(ConceptVariant("http://en.wikipedia.org/wiki/Titanic_(1997_film)"))
      )
    )

    val trainingSet = searchResults.map(sentSeq => {
      println("\n ------------------------------------------ \n")
      sentSeq.foreach(_.print())
      ContextElementConverter.convert(sentSeq.map(SentenceAnalizer.toBagOfWordsElement), contextSize)
    })

    val model = new Model[BaseElement, SkNNNode[BaseElement]]((label) => {
      new SkNNNodeImpl[BaseElement, PlainAverageStorage[BaseElement]](label, 1)(() => {
        new PlainAverageStorage[BaseElement](ElementMeasures.bagOfWordElementDistance)
      })
    })

    trainingSet.foreach(seq => model.processSequenceImpl(seq)(onto => List((onto.label, onto))))

    val sknn = new SkNN[BaseElement, SkNNNode[BaseElement]](model)


    val res = sknn.tag(target, 1)((elem, node) => {
      elem match {
        case contextElement: ContextElement => contextElement.mainElement match {
          case bow: BagOfWordElement => bow.label == node.label
          case _ => true
        }
        case _ => true
      }
    })


    println("\n =========== Result ========= \n")

    val recoveredResult1 = RecoverConcept.recover(target, model.initNode, res(0)._1)
    println(s"Weight: ${res(0)._2}")
    objs.zip(recoveredResult1).foreach({ case (obj, node) => println(s"${obj.tokens.mkString(" ")} => ${node.label}") })

  }

}
