package com.generall.resolver.filters

import ml.generall.nlp.ChunkRecord

/**
  * Created by generall on 13.09.16.
  */
trait MentionFilter {
  def filter(records: List[ChunkRecord], weights: List[Double]): Boolean
}
