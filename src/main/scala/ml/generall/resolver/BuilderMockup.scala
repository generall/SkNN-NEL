package ml.generall.resolver

import ml.generall.resolver.dto.{EnrichedSentence, MentionSearchResult}
import ml.generall.resolver.tools.SaveTools

/**
  * Created by generall on 18.03.17.
  */
object BuilderMockup extends BuilderInterface{

  type HrefStore = List[(String, List[EnrichedSentence])]

  type MentionStore = List[(String, MentionSearchResult)]

  val mentionMocks: Map[String, MentionSearchResult] = SaveTools.load[MentionStore](this.getClass.getResourceAsStream("/mention_mockup")).toMap

  val hrefMocks: Map[String, List[EnrichedSentence]] = SaveTools.load[HrefStore](this.getClass.getResourceAsStream("/href_mockup")).toMap

  override def searchMention(mention: String, leftContext: String, rightContext: String, mustWords: List[String]): MentionSearchResult = mentionMocks.getOrElse(mention, new MentionSearchResult(Nil)(x => true))

  override def searchMentionsByHref(href: String, leftContext: String, rightContext: String): Seq[EnrichedSentence] = hrefMocks(href)
}
