package ml.generall.resolver.dto

/**
  * Created by generall on 11.03.17.
  */
case class ConceptsAnnotation(
                               fromPos: Int,
                               toPos: Int,
                               concepts: List[ConceptVariant]
                             ) {}
