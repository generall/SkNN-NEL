package ml.generall.resolver


import scala.concurrent.duration._
import java.util

import com.sksamuel.elastic4s.{Hit, HitReader}
import com.sksamuel.elastic4s.ElasticDsl._
import ml.generall.isDebug
import ml.generall.resolver.filters.{DummyFilter, SvmFilter}
import ml.generall.elastic.Chunk
import ml.generall.nlp._
import ml.generall.resolver.dto._
import ml.generall.resolver.tools.Hyperparams

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

trait BuilderInterface {
  val searcher = Searcher

  val mentionFilter = SvmFilter


  def weightFu(record: ChunkRecord): Double = {
    LocalIDFDict.getIDF(record.lemma.toLowerCase) / Math.log(LocalIDFDict.totalWordCount)
  }

  def filterChunk(tokens: List[ChunkRecord]): Boolean = {
    val weights = tokens.map(weightFu)
    mentionFilter.filter(tokens, weights)
  }

  def searchMention(mention: String, leftContext: String = "", rightContext: String = "", mustWords: List[String] = Nil): MentionSearchResult

  def searchMentionsByHref(href: String, leftContext: String = "", rightContext: String = ""): Seq[EnrichedSentence]
}

/**
  * Created by generall on 27.08.16.
  */

object Builder extends BuilderInterface {


  /**
    * Search for possible concepts of mention
    *
    * @param mention      mention text
    * @param leftContext  left context
    * @param rightContext right context
    * @return concept variants
    */
  override def searchMention(mention: String, leftContext: String = "", rightContext: String = "", mustWords: List[String] = Nil): MentionSearchResult = {
    implicit object MentionHitAs extends HitReader[EnrichedMention] {

      override def read(hit: Hit): Either[Throwable, EnrichedMention] = {
        val mentions = hit.sourceAsMap("mentions").asInstanceOf[util.List[util.Map[String, AnyRef]]]
        val origMention = mentions.find(mention => mention("resolver").asInstanceOf[String] == ConceptVariant.WIKILINKS_RESOLVER)
        origMention match {
          case Some(x: util.Map[String, AnyRef]) => {
            Right(mapToMention(x))
          }
          case None => Left(new RuntimeException("Can not parse"))
        }
      }
    }

    val mustWordRegexps = mustWords.map(mustWord => ("(?i)" + mustWord).r)

    val variants = searcher.customSearch(client => {
      client.execute {
        search("sknn-data") query {
          nestedQuery("mentions") query {
            boolQuery().should(
              matchQuery("mentions.text", mention),
              matchQuery("mentions.context.left", leftContext),
              matchQuery("mentions.context.right", rightContext)
            ).must(
              matchQuery("mentions.resolver", ConceptVariant.WIKILINKS_RESOLVER)
            )
          } scoreMode "Max"
        } limit Hyperparams.SEARCH_LIMIT_MENTION
      }.await(60.seconds)
    }).groupBy(_._1.concepts.head.concept)
      .map {
        case (concept, pairs) =>
          (concept, pairs
            .filter {
              case (enrichedMention, _) =>
                mustWordRegexps.forall(mustWordRegex => mustWordRegex.findFirstIn(enrichedMention.text).nonEmpty)
            }
            .map(_._2))
      }
      .filter(_._2.nonEmpty)
      .map {
        case (concept, pairs) => calcStat(concept, pairs)
      }

    new MentionSearchResult(variants)(filterResult)
  }

  /**
    * Search for sentences with target concept
    *
    * @param href         target concept as wikipedia link
    * @param leftContext  left context
    * @param rightContext right context
    * @return Sentence with all mentions
    */
  override def searchMentionsByHref(href: String, leftContext: String = "", rightContext: String = ""): Seq[EnrichedSentence] = {
    implicit object SentenceHitAs extends HitReader[EnrichedSentence] {
      override def read(hit: Hit): Either[Throwable, EnrichedSentence] = {

        val hitMap = hit.sourceAsMap

        val mentions = hitMap("mentions")
          .asInstanceOf[util.List[util.Map[String, AnyRef]]]
          .map(x => x("id").asInstanceOf[Int] -> mapToMention(x)).toMap


        val chunks = hitMap.getOrElse("parse_result", new util.ArrayList)
          .asInstanceOf[util.List[util.Map[String, AnyRef]]]
          .groupBy(_ ("group").asInstanceOf[Int])
          .toList
          .sortBy(_._1)
          .map(x => EnrichedChunk(x._2.map(token => {
            val tkn = mapToToken(token)
            val tokenMentions = token
              .getOrDefault("mentions", new util.ArrayList[Int])
              .asInstanceOf[util.List[Int]].map(mentions(_))
              .toList
            tkn.mentions = tokenMentions
            tkn
          }
          ).toList))

        Right(EnrichedSentence(
          mentions = mentions.values.toList,
          chunks = chunks,
          sent = hitMap("sent").asInstanceOf[String]
        ))
      }
    }

    searcher.customSearch(client => {
      client.execute {
        search("sknn-data") query {
          nestedQuery("mentions") query {
            boolQuery().should(
              matchQuery("mentions.context.left", leftContext),
              matchQuery("mentions.context.right", rightContext)
            ).must(
              matchQuery("mentions.concepts.link", href),
              matchQuery("mentions.resolver", ConceptVariant.WIKILINKS_RESOLVER)
            )
          } scoreMode "Max"
        } limit Hyperparams.SEARCH_LIMIT_HREF
      }.await(60.seconds)
    }).map(_._1)
  }

  /**
    * Convert util.Map to ConceptVariant
    *
    * @param x
    * @return
    */
  def mapToConcept(x: util.Map[String, AnyRef]): ConceptVariant = ConceptVariant(
    x("link").asInstanceOf[String],
    x("hits").asInstanceOf[Int],
    x("avgScore").asInstanceOf[Double],
    x("maxScore").asInstanceOf[Double],
    x("minScore").asInstanceOf[Double],
    x("avgNorm").asInstanceOf[Double],
    x("avgSoftMax").asInstanceOf[Double]
  )

  def mapToMention(x: util.Map[String, AnyRef]): EnrichedMention = EnrichedMention(
    x("text").asInstanceOf[String],
    x("resolver").asInstanceOf[String],
    x("concepts").asInstanceOf[util.List[util.Map[String, AnyRef]]].map(mapToConcept).toList
  )

  def mapToToken(x: util.Map[String, AnyRef]): EnrichedToken = EnrichedToken(
    word = x("token").asInstanceOf[String],
    lemma = x("lemma").asInstanceOf[String],
    pos = x("pos_tag").asInstanceOf[String],
    parseTag = x("parserTag").asInstanceOf[String],
    mentions = Nil
  )

  def calcStat(concept: String, list: Iterable[Float]): ConceptVariant = {
    val size = list.size
    ConceptVariant(
      concept = concept,
      count = size,
      avgScore = list.foldLeft(0.0)(_ + _) / size,
      maxScore = list.max,
      minScore = list.min
    )
  }

  /**
    * Predicate for skipping rare concepts
    *
    * @param x concept variant
    * @return
    */
  def filterResult(x: ConceptVariant): Boolean = {
    if (x.count <= searcher.thresholdCount) {
      if (isDebug()) println(s"Filter concept: ${x.concept}")
      false
    } else true
  }

  def makeTrainFromRecords(records: List[ChunkRecord], state: String, concepts: List[ConceptVariant]): TrainObject = {
    TrainObject(records.map(x => (x.lemma, 1.0)), state, concepts)
  }
}

class ExamplesBuilder {

  val splitter: SentenceSplitter = LocalSplitter

  val parser: CoreNLPTools = LocalCoreNLP

  var builder: BuilderInterface = Builder

  /**
    * Build train set for concept (with context)
    *
    * @param concept link to DBpedia
    * @param leftContext
    * @param rightContext
    * @return List of train examples converted to TrainObject
    */
  def build(concept: String, leftContext: String = "", rightContext: String = ""): List[List[TrainObject]] = {
    val searchRes = builder.searchMentionsByHref(concept, leftContext, rightContext)

    FileLogger.logToFile("/tmp/learning.log", concept)
    FileLogger.logToFile("/tmp/learning.log", "")

    searchRes.map(enrichedSentenceToTrain).toList
  }

  /**
    * Convert enriched sentence to train sequence
    *
    * @param sentence Enriched sentence from database
    * @return
    */
  def enrichedSentenceToTrain(sentence: EnrichedSentence): List[TrainObject] = {
    sentence.chunks.map(chunk => {
      var allConcepts = chunk.getAllMentions
        .filter(_.resolver != ConceptVariant.WIKILINKS_RESOLVER)
        .flatMap(_.concepts)
      val wikilinksMention = chunk.getAllMentions.find(x => x.resolver == ConceptVariant.WIKILINKS_RESOLVER)
      val state = (wikilinksMention match {
        case None => selectState(allConcepts) // If no wikilink mention in chunk
        case Some(x) =>
          val conceptVar = x.concepts.headOption.map(_.copy(resolver = ConceptVariant.WIKILINKS_RESOLVER))
          allConcepts = conceptVar.toList ++ allConcepts
          conceptVar.map(_.concept) // set wikilink concept as state
      }).getOrElse(chunk.tokens.head.parseTag)

      TrainObject(
        tokens = chunk.tokens.map(x => (x.lemma, 1.0)),
        state = state,
        concepts = allConcepts,
        resolver = if (wikilinksMention.isDefined) ConceptVariant.WIKILINKS_RESOLVER else ConceptVariant.ELASTIC_RESOLVER
      )
    })
  }

  /**
    * Make list of train objects from grouped chunks (used for target sentence)
    *
    * @param groups groups of chunks
    * @return
    */
  def makeTrain(groups: Iterable[(String /* state */ , List[ChunkRecord])]): List[TrainObject] = {
    val pattern = "^(NP.*)".r
    groups.par.map(group => {
      val (state, tokens) = group
      state match {
        case pattern(_) =>
          val text = tokens.map(_.word).mkString(" ")
          val weightedTokens = tokens.map(x => (x, builder.weightFu(x)))
          val weights = weightedTokens.map(_._2)
          val mostValuableWords = weightedTokens.sortBy(-_._2).take(tokens.size / 2).map(_._1.lemma)
          val variants = if (builder.mentionFilter.filter(tokens, weights))
            builder.searchMention(text, mustWords = mostValuableWords).stats
          else {
            if (isDebug()) println(s"Filter mention: $text")
            Nil
          }
          TrainObject(tokens.zip(weights).map(x => (x._1.lemma, x._2)), state, variants)
        case _ =>
          val weights = tokens.map(builder.weightFu)
          TrainObject(tokens.zip(weights).map(x => (x._1.lemma, x._2)), state, Nil)
      }
    }).toList
  }

  /**
    * Funtion for selection state of TrainObject by it's context
    *
    * @param concepts
    * @return
    */
  def selectState(concepts: List[ConceptVariant]): Option[String] = concepts match {
    case Nil => None
    case _ => Some(concepts.maxBy(_.count).concept)
  }

}