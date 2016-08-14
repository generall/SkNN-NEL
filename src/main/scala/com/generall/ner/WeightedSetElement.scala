package com.generall.ner

import com.generall.sknn.model.storage.elements.SetElement

/**
  * Created by generall on 13.08.16.
  */
trait WeightedSetElement extends SetElement{

  /**
    * @return sorted by String feature list
    */
  def features: Map[String, Double]
}
