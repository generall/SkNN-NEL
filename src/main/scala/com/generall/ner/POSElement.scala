package com.generall.ner

/**
  * Created by generall on 13.08.16.
  */
class POSElement(_label: String, singleOutput: String, tag: POSTag) extends WeightedSetElement {


  override def features: Map[String, Double] = Map(tag.word -> 1.0, tag.tag -> 1.0)

  override var label: String = _label
  override var output: Set[String] = Set(singleOutput)

}
