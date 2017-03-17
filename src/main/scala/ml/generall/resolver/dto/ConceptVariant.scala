package ml.generall.resolver.dto

import ml.generall.resolver.tools.ProbTools

/**
  * Class for describing concept variant
  *
  * Created by generall on 15.03.17.
  */
case class ConceptVariant(
                           concept: String,
                           count: Int = 1,
                           avgScore: Double = 1.0,
                           maxScore: Double = 1.0,
                           minScore: Double = 1.0,
                           var avgNorm: Double = 0.0,
                           var avgSoftMax: Double = 0.0,
                           var resolver: String = ""
                         ) {
  /**
    * Returns variant weight.
    * @return weight. Max = 1.0 (wikilinks)
    */
  def getWeight: Double = if (resolver == ConceptVariant.WIKILINKS_RESOLVER) 1.0 else ProbTools.logistic( avgScore * count, 20.0) // TODO: Hyperparam
}

object ConceptVariant {
  val WIKILINKS_RESOLVER = "wikilink"
  val ELASTIC_RESOLVER = "elastic"
}
