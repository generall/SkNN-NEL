package ml.generall.resolver

import java.util

import com.sksamuel.elastic4s.{Hit, HitReader}
import com.sksamuel.elastic4s.ElasticDsl._
import ml.generall.isDebug
import ml.generall.resolver.filters.{DummyFilter, SvmFilter}
import ml.generall.elastic.{Chunk, ConceptVariant}
import ml.generall.nlp._
import ml.generall.resolver.dto._

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._


/**
  * Created by generall on 27.08.16.
  */

object Builder {

  val searcher = Searcher

  val mentionFilter = SvmFilter


  def weightFu(record: ChunkRecord): Double = {
    LocalIDFDict.getIDF(record.lemma.toLowerCase) / Math.log(LocalIDFDict.totalWordCount)
  }

  def filterChunk(tokens: List[ChunkRecord]): Boolean = {
    val weights = tokens.map(weightFu)
    mentionFilter.filter(tokens, weights)
  }

  /**
    * Make list of train objects from grouped chunks
    *
    * @param groups groups of chunks
    * @return
    */
  def makeTrain(groups: Iterable[(String /* state */ , List[ChunkRecord])]): List[TrainObject] = {
    groups.map(group => {
      val (state, tokens) = group
      val pattern = "^(NP.*)".r
      state match {
        case pattern(_) => {
          val text = tokens.map(_.word).mkString(" ")
          val weights = tokens.map(weightFu)
          val variants = if (mentionFilter.filter(tokens, weights))
            searchMention(text).stats
          else {
            if (isDebug()) println(s"Filter mention: $text")
            Nil
          }
          TrainObject(tokens.zip(weights).map(x => (x._1.lemma, x._2)), state, variants)
        }
        case _ =>
          val weights = tokens.map(weightFu)
          TrainObject(tokens.zip(weights).map(x => (x._1.lemma, x._2)), state, Nil)
      }
    }).toList
  }


  def searchMention(mention: String, leftContext: String = "", rightContext: String = ""): MentionSearchResult = {
    implicit object MentionHitAs extends HitReader[EnrichedMention] {

      override def read(hit: Hit): Either[Throwable, EnrichedMention] = {
        val mentions = hit.sourceAsMap("mentions").asInstanceOf[util.List[util.Map[String, AnyRef]]]
        val origMention = mentions.find(mention => mention("resolver").asInstanceOf[String] == "wikilink")
        origMention match {
          case Some(x: util.Map[String, AnyRef]) => {
            Right(mapToMention(x))
          }
          case None => Left(new RuntimeException("Can not parse"))
        }
      }
    }

    val variants = searcher.customSearch(client => {
      client.execute {
        search("sknn-data") query {
          nestedQuery("mentions") query {
            boolQuery().should(
              matchQuery("mentions.text", mention),
              matchQuery("mentions.context.left", leftContext),
              matchQuery("mentions.context.right", rightContext)
            ).must(
              matchQuery("mentions.resolver", "wikilink")
            )
          } scoreMode "Max"
        }
      }.await
    }).groupBy(_._1.concepts.head.concept)
      .map({ case (concept, pairs) => calcStat(concept, pairs.map(_._2)) })

    new MentionSearchResult(variants)(filterResult)
  }

  def searchMentionsByHref(href: String, leftContext: String = "", rightContext: String = ""): Seq[EnrichedSentence] = {
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
              matchQuery("mentions.resolver", "wikilink")
            )
          } scoreMode "Max"
        }
      }.await
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

  val searcher = Searcher

  val splitter: SentenceSplitter = LocalSplitter

  val parser: CoreNLPTools = LocalCoreNLP

  def convertWithoutAnnotations(records: List[ChunkRecord], listBuffer: ListBuffer[TrainObject]) = {
    val groups = records.groupBy(record => (record.parseTag, 0 /* record.ner*/ , record.groupId))
    groups.toSeq.sortBy(x => x._1._3).foreach(group => {
      val ((parserTag, ner, _), groupRecords) = group
      /**
        * #StateDefinition
        */
      val trainObject = Builder.makeTrainFromRecords(groupRecords, s"${parserTag}" /* _$ner */ , Nil)
      listBuffer.append(trainObject)
    })
  }

  def convertRecords(records: List[ChunkRecord], annotations: List[ConceptsAnnotation], listBuffer: ListBuffer[TrainObject]): Unit = {
    annotations match {
      case head :: tail => {
        val (beforeAnnotation, withAnnotation) = records.span(record => record.endPos < head.fromPos)
        convertWithoutAnnotations(beforeAnnotation, listBuffer)
        val (annotated, afterAnnotation) = withAnnotation.span(record => record.beginPos < head.toPos)

        /**
          * Creating of state label here #StateDefinition
          */
        val trainObject = Builder.makeTrainFromRecords(annotated, s"${annotated.head.parseTag}" /* _${annotated.head.ner}" */ , head.concepts)
        listBuffer.append(trainObject)
        convertRecords(afterAnnotation, tail, listBuffer)
      }
      case Nil => {
        convertWithoutAnnotations(records, listBuffer)
      }
    }
  }

  /**
    *
    * @param sentence
    * @param annotations of sentence. Must not overlap!
    * @return
    */
  def buildFromAnnotations(sentence: String, annotations: List[ConceptsAnnotation]): List[TrainObject] = {
    val buf = new ListBuffer[TrainObject]
    val records = parser.process(sentence)
    convertRecords(records, annotations, buf)
    buf.toList
  }


  def buildFromMention(firstChunk: Chunk, middleChunk: Chunk, lastChunk: Chunk, concepts: List[ConceptVariant]): List[TrainObject] = {

    val firstChunkText = firstChunk.text.replaceAll("\\P{InBasic_Latin}", "")
    val middleChunkText = middleChunk.text.replaceAll("\\P{InBasic_Latin}", "")
    val lastChunkText = lastChunk.text.replaceAll("\\P{InBasic_Latin}", "")

    val startMentionPos = firstChunkText.length + 1
    val endMentionPos = middleChunkText.length + startMentionPos + 1

    val text = firstChunkText ++ " " ++ middleChunkText ++ " " ++ lastChunkText

    val sentenceRange = splitter.getSentence(text, (startMentionPos, endMentionPos)) match {
      case None => {
        throw UnparsableException(text, startMentionPos, endMentionPos)
      }
      case x => x.get
    }
    val sentence = text.substring(sentenceRange._1, sentenceRange._2)
    val annotations = List(ConceptsAnnotation(startMentionPos - sentenceRange._1, endMentionPos - sentenceRange._1, concepts))
    buildFromAnnotations(sentence, annotations)
  }

  /**
    * Build train set for concept (with context)
    *
    * @param concept link to DBpedia
    * @param leftContext
    * @param rightContext
    * @return List of train examples converted to TrainObject
    */
  def build(concept: String, leftContext: String = "", rightContext: String = ""): List[List[TrainObject]] = {
    val searchRes = Builder.searchMentionsByHref(concept, leftContext, rightContext)
    val count = searchRes.size

    FileLogger.logToFile("/tmp/learning.log", concept)
    FileLogger.logToFile("/tmp/learning.log", "")

    searchRes.map(enrichedSentenceToTrain).toList
  }

  /**
    * Convert enriched sentence to train sequence
    *
    * @param sentence
    * @return
    */
  def enrichedSentenceToTrain(sentence: EnrichedSentence): List[TrainObject] = {
    sentence.chunks.map(chunk => {
      TrainObject(
        tokens = chunk.tokens.map(x => (x.lemma, 1.0)),
        state = chunk.tokens.head.parseTag, // TODO: create more advanced state here
        concepts = chunk.getAllMentions.flatMap(_.concepts)
      )
    })
  }

}

//boolQuery.should(
//matchQuery("mentions.text", mention),
//matchQuery("mentions.resolver", "wikilink")
//)
