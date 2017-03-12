package ml.generall.resolver.dto

import ml.generall.elastic.ConceptVariant
import ml.generall.nlp.ChunkRecord

/**
  * This file contains classes used for transfer and transform entities from enriched dataset
  * Created by generall on 11.03.17.
  */
case class EnrichedSentence (
                            sent: String,
                            chunks: List[EnrichedChunk],
                            mentions: List[EnrichedMention]
                            ){

}

case class EnrichedMention (
                           text: String,
                           resolver: String,
                           concepts: List[ConceptVariant]
                           ){

}


case class EnrichedChunk(
                        tokens: List[EnrichedToken]
                        ){
  def getAllMentions: List[EnrichedMention] = tokens.flatMap(_.mentions).distinct
}

case class EnrichedToken(
                          word: String,
                          lemma: String,
                          pos: String,
                          //ner: String,
                          parseTag: String,
                          var mentions: List[EnrichedMention]
                        )