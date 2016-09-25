package ml.generall.ner.elements

import ml.generall.sknn.model.storage.elements.SetElement

/**
  * Created by generall on 13.08.16.
  */
trait WeightedSetElement extends SetElement{

  /**
    * @return sorted by String feature list
    */
  def features: Map[String, Double]
}

object EmptyWeightedElement extends WeightedSetElement {
  /**
    * @return sorted by String feature list
    */
  override def features: Map[String, Double] = Map()

  override var output: Set[String] = null
  override var label: String = null
}