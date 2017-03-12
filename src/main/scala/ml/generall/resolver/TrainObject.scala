package ml.generall.resolver

import ml.generall.elastic.ConceptVariant

/**
  * Stores chunk of tokens, state and possible concepts
  * Created by generall on 27.08.16.
  */
case class TrainObject(
                        tokens:Iterable[(String, Double /* term frequency */)],
                        state: String,
                        concepts: Iterable[ConceptVariant]
                 ) {

  def print() = {
    println(s"state: $state: $tokens")
    concepts.foreach(concept => {
      println(s"\t${concept.concept} ${concept.count}")
    })
  }

}
