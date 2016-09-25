package ml.generall.resolver

import ml.generall.elastic.ConceptVariant

/**
  * Created by generall on 27.08.16.
  */
case class TrainObject(
                   val tokens:Iterable[(String, Double /* term frequency */)],
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
