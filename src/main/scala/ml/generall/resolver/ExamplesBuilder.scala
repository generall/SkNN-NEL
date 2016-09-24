package com.generall.resolver

import com.generall.resolver.filters.DummyFilter
import ml.generall.elastic.{Chunk, ConceptVariant}
import ml.generall.nlp._

import scala.collection.mutable.ListBuffer

/**
  * Created by generall on 27.08.16.
  */

object Builder {

  val searcher = Searcher

  val mentionFilter = DummyFilter


  def weightFu(record: ChunkRecord): Double = {
    LocalIDFDict.getIDF(record.lemma.toLowerCase) / Math.log(LocalIDFDict.totalWordCount)
  }

  /**
    * Make list of train objects from grouped chunks
    *
    * @param groups groups of chunks
    * @return
    */
  def makeTrain(groups: Iterable[(String, List[ChunkRecord])]): List[TrainObject] = {
    groups.map(group => {
      val (state, tokens) = group
      val pattern = "^(NP.*)".r
      state match {
        case pattern(c) => {
          val text = tokens.map(_.word).mkString(" ")
          val weights = tokens.map(weightFu)
          val variants = if (mentionFilter.filter(tokens, weights)) searcher.findMentions(text).stats else Nil
          new TrainObject(tokens.zip(weights).map(x => (x._1.lemma, x._2)), state, variants)
        }
        case _ =>
          val weights = tokens.map(weightFu)
          new TrainObject(tokens.zip(weights).map(x => (x._1.lemma, x._2)), state, Nil)
      }
    }).toList
  }

  def makeTrainFromRecords(records: List[ChunkRecord], state: String, concepts: List[ConceptVariant]): TrainObject = {
    new TrainObject(records.map(x => (x.lemma, 1.0)), state, concepts)
  }
}


case class ConceptsAnnotation(
                               val fromPos: Int,
                               val toPos: Int,
                               val concepts: List[ConceptVariant]
                             ) {}

class ExamplesBuilder {

  val searcher = Searcher

  val splitter: SentenceSplitter = LocalSplitter

  val parser: CoreNLPTools = LocalCoreNLP

  def convertWithoutAnnotations(records: List[ChunkRecord], listBuffer: ListBuffer[TrainObject]) = {
    val groups = records.groupBy(record => (record.parseTag, record.ner, record.groupId))
    groups.toSeq.sortBy(x => x._1._3).foreach(group => {
      val ((parserTag, ner, _), groupRecords) = group
      /**
        * #StateDefinition
        */
      val trainObject = Builder.makeTrainFromRecords(groupRecords, s"${parserTag}" /* _$ner */, Nil)
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
        val trainObject = Builder.makeTrainFromRecords(annotated, s"${annotated.head.parseTag}" /* _${annotated.head.ner}" */, head.concepts)
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
    val startMentionPos = firstChunk.text.length + 1
    val endMentionPos = middleChunk.text.length + startMentionPos + 1
    val text = firstChunk.text ++ " " ++ middleChunk.text ++ " " ++ lastChunk.text
    val sentenceRange = splitter.getSentence(text, (startMentionPos, endMentionPos)) match {
      case None => {
        throw new UnparsableException(text, startMentionPos, endMentionPos)
      }
      case x => x.get
    }
    val sentence = text.substring(sentenceRange._1, sentenceRange._2)
    val annotations = List(ConceptsAnnotation(startMentionPos - sentenceRange._1, endMentionPos - sentenceRange._1, concepts))
    buildFromAnnotations(sentence, annotations)
  }

  def build(concept: String, leftContext: String = "", rightContext: String = ""): List[List[TrainObject]] = {
    val searchRes = searcher.findHrefWithContext(concept, leftContext, rightContext)
    val count = searchRes.size

    FileLogger.logToFile("/tmp/learning.log", concept)
    FileLogger.logToFile("/tmp/learning.log", "")

    val conceptVariant = List(ConceptVariant(
      concept = concept,
      count = count,
      avgScore = 1.0,
      avgNorm = 1.0,
      avgSoftMax = 1.0,
      maxScore = 1.0,
      minScore = 1.0
    ))

    searchRes.filter(item => {
      /* filtering results with empty context */
      val res = item.chunks match {
        case List(firstChunk, _, lastChunk) => {
          !(firstChunk.text.isEmpty && lastChunk.text.isEmpty)
        }
      }
      res
    }).map(item => {
      item.chunks match {
        case List(firstChunk, middleChunk, lastChunk) => {

          FileLogger.logToFile("/tmp/learning.log", firstChunk.text ++ " | " ++ middleChunk.text ++ " | " ++ lastChunk.text)
          try {
            buildFromMention(firstChunk, middleChunk, lastChunk, conceptVariant)
          } catch {
            case ex: UnparsableException => println(ex); Nil
          }
        } /* end case */
      } /* end match */
    }).filter(_ != Nil) /* end map*/
  }

}
