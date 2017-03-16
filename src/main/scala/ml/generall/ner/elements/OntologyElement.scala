package ml.generall.ner.elements

import ml.generall.ontology.base.GraphClient
import ml.generall.ontology.structure.{Concept, TraversalFactory}

import scala.collection.mutable

/**
  * Created by generall on 13.08.16.
  */
class OntologyElement(conceptUrl: String, _label: String = null, conceptWeight: Double = 1.0) extends WeightedSetElement {
  override var label: String = if (_label == null) conceptUrl else _label
  var concept: Concept = OntologyElement.constructConcept(new Concept(conceptUrl))

  def features(_threshold: Double): Map[String, Double] = {
    concept.ontology.getTop(_threshold).map(x => (x.category, x.weight * conceptWeight)).toMap
  }

  val sortedFeatureMap: Map[String, Double] = features(OntologyElement.threshold)

  override var output: Set[String] = sortedFeatureMap.keySet

  override def features: Map[String, Double] = sortedFeatureMap

}

object OntologyElement {
  val factory = new TraversalFactory(GraphClient)

  val threshold = 0.2 // TODO: hyper parameter

  def constructConcept(concept: Concept, _threshold: Double = threshold): Concept = {
    val traversal = factory.constructConcept(concept.categories, _threshold)
    concept.ontology = traversal
    concept
  }

  /**
    * first should be withDefaultValue(0.0)
    *
    * @param first accumulator
    * @param second map
    * @return accumulator
    */
  def joinFeatures(first: mutable.Map[String, Double], second: Map[String, Double]): mutable.Map[String, Double] = {
    second.foreach { case (k, v) => first(k) += v }
    first
  }

}