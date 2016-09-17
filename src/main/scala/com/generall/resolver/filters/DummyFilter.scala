package com.generall.resolver.filters

import ml.generall.nlp.ChunkRecord

/**
  * Created by generall on 13.09.16.
  */
object DummyFilter extends MentionFilter{
  override def filter(records: List[ChunkRecord], weights: List[Double]): Boolean = {
    val params = (weights.sum, records.size, weights.sum / records.size)
    if(params._2 > 5) return false
    if(params._1 < 0.5) return false
    true
  }
}
