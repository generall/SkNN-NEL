package com.generall.ner.elements

/**
  * Created by generall on 04.09.16.
  */
class BagOfWordElement(
                        words: Map[String, Double],
                        _label: String,
                        singleOutput: String = null) extends WeightedSetElement {
  /**
    * @return sorted by String feature list
    */
  override def features: Map[String, Double] = words

  override var output: Set[String] = Set(if(singleOutput == null) _label else singleOutput)
  override var label: String = _label
}
