package ml.generall.resolver

import ml.generall.ner.elements._
import ml.generall.ner.{ElementMeasures, RecoverConcept}
import ml.generall.sknn.SkNN
import ml.generall.sknn.model.storage.PlainAverageStorage
import ml.generall.sknn.model.storage.elements.BaseElement
import ml.generall.sknn.model.{Model, SkNNNode, SkNNNodeImpl}
import ml.generall.elastic.ConceptVariant

import scala.collection.mutable


object SentenceAnalizer {


  def getConceptsToLearn(objList: List[TrainObject], contextSize: Int): List[(String, String, String)] = {

    var res: List[(String, String, String)] = Nil
    val context = ContextElementConverter.convertContext(objList, contextSize).toList
    context.foreach({
      case (leftContext, elem, rightContext) =>
      if (elem.concepts.size > 1) {
        val leftContextString = leftContext.flatMap(elem => elem.tokens.map(_._1)).mkString(" ")
        val rightContextString = rightContext.flatMap(elem => elem.tokens.map(_._1)).mkString(" ")
        elem.concepts.foreach(concept => res = (leftContextString, concept.concept, rightContextString) :: res)
      }
    })
    res
  }

  def wikiToDbpedia(wikilink: String): String = {
    wikilink.replaceAllLiterally("en.wikipedia.org/wiki", "dbpedia.org/resource")
  }

  def toBagOfWordsElement(obj: TrainObject): BaseElement = {
    val element = new BagOfWordElement(obj.tokens.map(lemma => {
      (lemma._1, lemma._2)
    }).toMap, obj.state)
    obj.concepts match {
      case Nil => element
      case List(concept) => {
        val multi = new MultiElement[WeightedSetElement]
        val onto = new OntologyElement(SentenceAnalizer.wikiToDbpedia(concept.concept))
        multi.addElement(onto)
        multi.addElement(element)
        multi.label = multi.genLabel
        multi
      }
      case disambiguation: Iterable[ConceptVariant] => {
        val multi = new MultiElement[WeightedSetElement]
        disambiguation
          .view
          .map(x => new OntologyElement(SentenceAnalizer.wikiToDbpedia(x.concept)))
          .foreach(multi.addElement)
        multi.addElement(element)
        multi.label = multi.genLabel
        multi
      }
    }
  }

}

/**
  * Created by generall on 27.08.16.
  */
class SentenceAnalizer {


  val contextSize = 5
  val searcher = Searcher
  val parser = LocalCoreNLP
  val exampleBuilder = new ExamplesBuilder

  def prepareSentence(sentence: String): List[TrainObject] ={
    val parseRes = parser.process(sentence)

    val groups = parseRes.zipWithIndex
      .groupBy({case (record, idx) => (record.parseTag, record.ner, record.groupId)})
      .toList
      .sortBy(x => x._2.head._2)
      .map(pair => (s"${pair._1._1}" /* _${pair._1._2} */, pair._2.map(_._1))) // creation of state

    Builder.makeTrain(groups)
  }


  def analyse(sentence: String) = {

    /**
      * Prepare target sentence
      */

    val objs = prepareSentence(sentence)


    objs.foreach(_.print())

    val target: List[ContextElement] = ContextElementConverter.convert(objs.map(SentenceAnalizer.toBagOfWordsElement), contextSize)

    /**
      * All concepts with disambiguation
      */
    val conceptsToLearn: List[(String, String, String)] = SentenceAnalizer.getConceptsToLearn(objs, contextSize)


    println("conceptsToLearn: ")
    conceptsToLearn.foreach(println)


    /**
      * Prepare training set from disambiguation
      */
    val searchResults: List[List[TrainObject]] = conceptsToLearn.flatMap(x => exampleBuilder.build(x._2, x._1, x._3))

    val trainingSet = searchResults.map(sentSeq => {
      ContextElementConverter.convert(sentSeq.map(SentenceAnalizer.toBagOfWordsElement), contextSize)
    })

    val model = new Model[BaseElement, SkNNNode[BaseElement]]((label) => {
      new SkNNNodeImpl[BaseElement, PlainAverageStorage[BaseElement]](label, 1)(() => {
        new PlainAverageStorage[BaseElement](ElementMeasures.bagOfWordElementDistance)
      })
    })

    println(s"trainingSet.size: ${trainingSet.size}")

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

    val recoveredResult1 = RecoverConcept.recover(target, model.initNode, res(0)._1)
    println(s"Weight: ${res(0)._2}")
    objs.zip(recoveredResult1).foreach({ case (obj, node) => println(s"${obj.tokens.mkString(" ")} => ${node.label}") })

    /*
    val recoveredResult2 = RecoverConcept.recover(target, model.initNode, res(1)._1)
    println(s"Weight: ${res(1)._2}")
    objs.zip(recoveredResult2).foreach({ case (obj, node) => println(s"${obj.tokens.mkString(" ")} => ${node.label}")})
    */
  }

}
