package com.generall.resolver

import com.generall.ner.{RecoverConcept, ElementMeasures}
import com.generall.ner.elements._
import com.generall.sknn.SkNN
import com.generall.sknn.model.storage.PlainAverageStorage
import com.generall.sknn.model.{SkNNNodeImpl, SkNNNode, Model}
import com.generall.sknn.model.storage.elements.BaseElement
import ml.generall.elastic.ConceptVariant

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

  def trainingObjectToElement(obj: TrainObject): BaseElement = {
    obj.concepts match {
      case Nil => new POSElement(POSTag(obj.text, obj.pos))
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

  def analyse(sentence: String) = {

    val contextSize = 5

    val groups = chunker.group(List(sentence)).head

    val objs = Builder.makeTrain(groups)

    val conceptsToLearn: List[String] = getConceptsToLearn(objs)

    val target = ContextElementConverter.convert(objs.map(trainingObjectToElement), contextSize)

    val trainingSet = conceptsToLearn.flatMap(exampleBuilder.build).map(sentSeq => {
      ContextElementConverter.convert(sentSeq.map(trainingObjectToElement), contextSize)
    })

    val model = new Model[BaseElement, SkNNNode[BaseElement]]((label) => {
      new SkNNNodeImpl[BaseElement, PlainAverageStorage[BaseElement]](label, 1)( () => {
        new PlainAverageStorage[BaseElement](ElementMeasures.baseElementDistance)
      })
    })

    trainingSet.foreach(seq => model.processSequenceImpl(seq)(onto => List((onto.label, onto)) ))

    val sknn = new SkNN[BaseElement, SkNNNode[BaseElement]](model)

    val res = sknn.tag(target, 2)( (_, _) => true)

    val recoveredResult1 = RecoverConcept.recover(target, model.initNode, res(0)._1)
    val recoveredResult2 = RecoverConcept.recover(target, model.initNode, res(1)._1)

    println(s"Weight: ${res(0)._2}")
    recoveredResult1.foreach(node => println(node.label))
    println(s"Weight: ${res(1)._2}")
    recoveredResult2.foreach(node => println(node.label))

  }

}
