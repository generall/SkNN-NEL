package com.generall.resolver

import com.generall.ner.elements._
import com.generall.ner.{ElementMeasures, RecoverConcept}
import com.generall.sknn.SkNN
import com.generall.sknn.model.storage.PlainAverageStorage
import com.generall.sknn.model.storage.elements.BaseElement
import com.generall.sknn.model.{Model, SkNNNode, SkNNNodeImpl}
import ml.generall.elastic.ConceptVariant

import scala.collection.mutable


/**
  * Created by generall on 27.08.16.
  */
class SentenceAnalizer {
  val searcher = Searcher
  val chunker = LocalChunker
  val exampleBuilder = new ExamplesBuilder

  def getConceptsToLearn(objList: List[TrainObject]): List[String] = objList match {
    case Nil => Nil
    case head :: tail =>
      if (head.concepts.size > 1)
        head.concepts.map(_.concept).toList ++ getConceptsToLearn(tail)
      else
        getConceptsToLearn(tail)
  }

  def wikiToDbpedia(wikilink: String): String = {
    wikilink.replaceAllLiterally("en.wikipedia.org/wiki", "dbpedia.org/resource")
  }

  /**
    * Convert train object to SkNN-acceptable elements (construct ontology if needed)
    *
    * @param obj train object
    * @return sibling of BaseElement
    */
  def toElement(obj: TrainObject): BaseElement = {
    obj.concepts match {
      case Nil => new POSElement(POSTag(obj.tokens.mkString(" ") , obj.state))
      case List(concept) => new OntologyElement(wikiToDbpedia(concept.concept))
      case disambiguation: Iterable[ConceptVariant] => {
        val multi = new MultiElement[OntologyElement]
        disambiguation
          .view
          .map(x => new OntologyElement(wikiToDbpedia(x.concept)))
          .foreach(multi.addElement)
        multi
      }
    }
  }

  def toBagOfWordsElement(obj: TrainObject): BaseElement = {
    obj.concepts match {
      case Nil => new BagOfWordElement(obj.tokens.map(token => {
        (token, 1.0) // TODO: add TF weighting here
      }).toMap, obj.state)
      case List(concept) => new OntologyElement(wikiToDbpedia(concept.concept))
      case disambiguation: Iterable[ConceptVariant] => {
        val multi = new MultiElement[OntologyElement]
        disambiguation
          .view
          .map(x => new OntologyElement(wikiToDbpedia(x.concept)))
          .foreach(multi.addElement)
        multi
      }
    }
  }


  def printGraph(model: Model[BaseElement, SkNNNode[BaseElement]]) = {
    val seen = new mutable.HashSet[SkNNNode[BaseElement]]()
    var notSeen = List(model.initNode)
    while(notSeen.nonEmpty){
      notSeen match {
        case head :: tail => {
          notSeen = tail
          if(!seen.contains(head)){
            head.getOutgoingNodes.foreach(node =>{
              println(s" ${head.label} -> ${node.label} ")
              notSeen = node :: notSeen
            })
            seen.add(head)
          }
        }
      }
    }
  }


  def analyse(sentence: String) = {

    val contextSize = 5

    val groups = chunker.group(List(sentence)).head

    val objs = Builder.makeTrain(groups)

    /**
      * All concepts with disambiguation
      */
    val conceptsToLearn: List[String] = getConceptsToLearn(objs)


    println("conceptsToLearn: ")
    conceptsToLearn.foreach(println)

    val target = ContextElementConverter.convert(objs.map(toBagOfWordsElement), contextSize)

    val searchResults = conceptsToLearn.flatMap(exampleBuilder.build)

    val trainingSet = searchResults.map(sentSeq => {
      ContextElementConverter.convert(sentSeq.map(toBagOfWordsElement), contextSize)
    })

    val model = new Model[BaseElement, SkNNNode[BaseElement]]((label) => {
      new SkNNNodeImpl[BaseElement, PlainAverageStorage[BaseElement]](label, 1)( () => {
        new PlainAverageStorage[BaseElement](ElementMeasures.bagOfWordElementDistance)
      })
    })

    println(s"trainingSet.size: ${trainingSet.size}")

    trainingSet.foreach(seq => model.processSequenceImpl(seq)(onto => List((onto.label, onto)) ))

    val sknn = new SkNN[BaseElement, SkNNNode[BaseElement]](model)


    val res = sknn.tag(target, 1)((_, _) => true)

    val recoveredResult1 = RecoverConcept.recover(target, model.initNode, res(0)._1)
    println(s"Weight: ${res(0)._2}")
    objs.zip(recoveredResult1).foreach({ case (obj, node) => println(s"${obj.tokens.mkString(" ")} => ${node.label}")})

    /*
    val recoveredResult2 = RecoverConcept.recover(target, model.initNode, res(1)._1)
    println(s"Weight: ${res(1)._2}")
    objs.zip(recoveredResult2).foreach({ case (obj, node) => println(s"${obj.tokens.mkString(" ")} => ${node.label}")})
    */
  }

}
