package ml.generall.resolver

import ml.generall.common.StupidAssert
import ml.generall.ner.{ElementMeasures, RecoverConcept}
import ml.generall.ner.elements._
import ml.generall.sknn.SkNN
import ml.generall.sknn.model.storage.PlainAverageStorage
import ml.generall.sknn.model.{Model, SkNNNode, SkNNNodeImpl}
import ml.generall.sknn.model.storage.elements.BaseElement
import ml.generall.elastic.Chunk
import ml.generall.ontology.structure.Concept
import ml.generall.resolver.dto.ConceptVariant
import ml.generall.resolver.tools.Tools
import org.scalatest.FunSuite

import scala.collection.{immutable, mutable}

/**
  * Created by generall on 27.08.16.
  */
class SentenceAnalizerTest extends FunSuite {


  test("testGetTrainingSet") {
    val analizer = new SentenceAnalizer

    val ts = analizer.getTrainingSet(List(
      ("", "http://en.wikipedia.org/wiki/Batman", ""),
      ("", "http://en.wikipedia.org/wiki/Batman:_Year_One", "")
    ))

    val categories = Tools.time {
      analizer.getAllWeightedCategories(ts)
    }

    Tools.time {
      analizer.updateStates(ts, categories)
    }

    ts.foreach(seq => {
      seq.foreach(x => println(x.label))
      println("---")
    })

    //categories.toList.sortBy(- _._2).foreach{case (k,v) => println(s"$k \t| $v")}
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
    val str = "Titanic crashed into iceberg"
    val analyzer = new SentenceAnalizer

    analyzer.exampleBuilder.builder = BuilderMockup

    val trains = analyzer.prepareSentence(str)

    trains.foreach(_.print())
  }

  test("testProfilegetTrainingSet") {

    val analyzer = new SentenceAnalizer

    analyzer.exampleBuilder.builder = BuilderMockup


    val trainingSet: List[List[ContextElement]] = Tools.time(analyzer.getTrainingSet(List(("", "http://en.wikipedia.org/wiki/RMS_Titanic", ""))), "getTrainingSet")

    println(trainingSet.size)

  }

  test("testProfileBagOfWords") {

    val vars = List(
      "http://en.wikipedia.org/wiki/Titanic_(1997_film)",
      "http://en.wikipedia.org/wiki/RMS_Titanic",
      "http://en.wikipedia.org/wiki/Titanic",
      "http://en.wikipedia.org/wiki/Iceberg_Theory",
      "http://en.wikipedia.org/wiki/Iceberg_(fashion_house)",
      "http://en.wikipedia.org/wiki/Iceberg",
      "http://en.wikipedia.org/wiki/James_Cameron"
    )


    val bow = vars.map(x => Tools.time(new OntologyElement(SentenceAnalizer.wikiToDbpedia(x)), "OntologyElement " + x))

    print(bow.size)
  }

  test("testNewParser") {

    val analyzer = new SentenceAnalizer

    analyzer.exampleBuilder.builder = BuilderMockup

    val sentence = "Titanic hit iceberg"
    //val sentence = "James Cameron made Titanic"

    /**
      * Prepare target sentence
      */
    val objects = Tools.time(analyzer.prepareSentence(sentence), "prepareSentence")

    /**
      * Get context element description
      */
    val target: List[ContextElement] = Tools.time(analyzer.convertToContext(objects), "convertToContext")

    /**
      * All concepts with disambiguation
      */
    val conceptsToLearn: List[(String, String, String)] = Tools.time(SentenceAnalizer.getConceptsToLearn(objects, analyzer.contextSize), "getConceptsToLearn")

    /**
      * Prepare training set from disambiguation
      */

    val trainingSet: List[List[ContextElement]] = Tools.time(analyzer.getTrainingSet(conceptsToLearn), "getTrainingSet")

    /**
      * Update of the states
      */
    val categories = Tools.time(analyzer.getAllWeightedCategories(trainingSet), "getAllWeightedCategories")

    Tools.time(analyzer.updateStates(trainingSet, categories), "updateStates")


    // TODO: resolve concepts redirects
    // TODO: Filter valuable concepts

    /**
      * Learn model
      */
    val (sknn, model) = Tools.time(analyzer.learnModel(trainingSet), "learnModel")

    /**
      * Tag sequence, return possible combinations with weight
      * < List[(List[Node], Double)] >
      */
    val res = Tools.time(sknn.tag(target, 1)(analyzer.filterNodes), "tag")

    def printStates(l: List[BaseElement]) = {
      println("-----")
      l.foreach(x => println(x.label))
    }

    printStates(trainingSet(0))
    printStates(trainingSet(1))
    printStates(trainingSet(2))
    printStates(trainingSet(100))
    printStates(trainingSet(101))
    printStates(trainingSet(102))

    println(" ---------- ")

    res.head._1.foreach(node => println(node.label))


    val recoveredResult1 = RecoverConcept.recover(target, model.initNode, res.head._1)
    println(s"Weight: ${res.head._2}")
    objects.zip(recoveredResult1).foreach({ case (obj, node) => println(s"${obj.tokens.mkString(" ")} => ${node.label}") })

  }

  test("testModelBuilding") {
    val analyzer = new SentenceAnalizer

    val titanic = new MultiElement[BaseElement]
    titanic.addElement(new OntologyElement(SentenceAnalizer.wikiToDbpedia("http://en.wikipedia.org/wiki/Titanic_(1997_film)")))
    titanic.addElement(new OntologyElement(SentenceAnalizer.wikiToDbpedia("http://en.wikipedia.org/wiki/RMS_Titanic")))

    val iceberg = new MultiElement[BaseElement]
    iceberg.addElement(new OntologyElement(SentenceAnalizer.wikiToDbpedia("http://en.wikipedia.org/wiki/Iceberg_Theory")))
    iceberg.addElement(new OntologyElement(SentenceAnalizer.wikiToDbpedia("http://en.wikipedia.org/wiki/Iceberg")))

    val trainContext = ContextElementConverter.convert(List(titanic, iceberg), 1)

    assert(trainContext.size == 2)

    val (sknn, model) = analyzer.learnModel(List(trainContext))

    assert(model.nodes.size > 2)
  }

}
