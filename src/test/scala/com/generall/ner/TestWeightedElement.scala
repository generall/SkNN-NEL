package com.generall.ner

/**
  * Created by generall on 14.08.16.
  */
class TestWeightedElement(f: Map[String, Double]) extends WeightedSetElement{
  /**
    * @return sorted by String feature list
    */
  override def features: Map[String, Double] = f

  override var output: Set[String] = Set()
  override var label: String = null
}
