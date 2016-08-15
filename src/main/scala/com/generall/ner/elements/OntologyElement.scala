package com.generall.ner.elements

import com.generall.ontology.base.GraphClient
import com.generall.ontology.structure.{Concept, TraversalFactory}

/**
  * Created by generall on 13.08.16.
  */
class OntologyElement(conceptUrl: String, _label: String = null) extends WeightedSetElement{
  override var label: String = if(_label == null) conceptUrl else _label
  var concept: Concept = OntologyElement.constructConcept(new Concept(conceptUrl))

  def features(_threshold : Double): Map[String, Double] = {
      concept.ontology.getTop(_threshold).map(x => (x.category, x.weight)).toMap
  }

  val sortedFeatureMap = features(OntologyElement.threshold)

  override var output: Set[String] = sortedFeatureMap.keySet

  override def features: Map[String, Double] = sortedFeatureMap
}

object OntologyElement{
  val factory = new TraversalFactory(GraphClient)

  val threshold = 0.2 // TODO: hyper parameter

  def constructConcept(concept: Concept, _threshold : Double = threshold): Concept = {
    val traversal = factory.constructConcept(concept.categories, _threshold)
    concept.ontology = traversal
    concept
  }

}