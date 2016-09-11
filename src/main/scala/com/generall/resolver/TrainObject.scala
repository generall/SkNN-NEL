package com.generall.resolver

import ml.generall.elastic.ConceptVariant

/**
  * Created by generall on 27.08.16.
  */
class TrainObject(
                   val tokens:Iterable[String],
                   val state: String,
                   val concepts: Iterable[ConceptVariant]
                 ) {

  def print() = {
    println(s"state: $state: $tokens")
    concepts.foreach(concept => {
      println(s"\t${concept.concept} ${concept.count}")
    })
  }

}
