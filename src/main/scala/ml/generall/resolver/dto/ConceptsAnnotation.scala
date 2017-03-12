package ml.generall.resolver.dto

import ml.generall.elastic.ConceptVariant

/**
  * Created by generall on 11.03.17.
  */
case class ConceptsAnnotation(
                               fromPos: Int,
                               toPos: Int,
                               concepts: List[ConceptVariant]
                             ) {}
