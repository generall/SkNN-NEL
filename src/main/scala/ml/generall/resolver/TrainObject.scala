package ml.generall.resolver

import ml.generall.resolver.dto.ConceptVariant


/**
  * Stores chunk of tokens, state and possible concepts
  * Created by generall on 27.08.16.
  */
case class TrainObject(
                        tokens:Iterable[(String, Double /* term frequency */)],
                        state: String,
                        concepts: Iterable[ConceptVariant],
                        resolver: String = ""
                 ) {

  def print() = {
    println(s"state: $state: $tokens")
    concepts.foreach(concept => {
      println(s"\t${concept.concept} ${concept.count}\t ${concept.resolver}")
    })
  }

}
