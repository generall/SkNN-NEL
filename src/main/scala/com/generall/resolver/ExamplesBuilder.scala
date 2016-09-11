package com.generall.resolver

import ml.generall.elastic.{Chunk, ConceptVariant}
import ml.generall.nlp.{ChunkRecord, CoreNLPTools, SentenceSplitter, Chunker}

import scala.collection.mutable.ListBuffer

/**
  * Created by generall on 27.08.16.
  */

object Builder {

  val searcher = Searcher


  /**
    * Make list of train objects from grouped chunks
    *
    * @param groups groups of chunks
    * @return
    */
  def makeTrain(groups: List[(String, List[(String, (String, String))])]): List[TrainObject] = {
    groups.flatMap({ case (group, list) => {
      group match {
        case "NP" => {
          val text = list.map(_._1).mkString(" ")
          val variants = searcher.findMentions(text).stats
          List(new TrainObject(list.map(_._1), "group", variants))
        }
        case _ =>
          list.map(word => {
            val (token, (pos, chunkTag)) = word
            new TrainObject(List(token), pos, Nil)
          })
      }
    }
    })
  }

  def makePosTrain(groups: List[(String, List[(String, (String, String))])]): List[TrainObject] = {
    groups.flatMap({ case (group, list) => {
      group match {
        case "NP" => {
          val text = list.map(_._1)
          List(new TrainObject(text, "group", Nil))
        }
        case _ =>
          list.map(word => {
            val (token, (pos, chunkTag)) = word
            new TrainObject(List(token), pos, Nil)
          })
      }
    }
    })
  }

  def makeTrainFromRecords(records: List[ChunkRecord], state: String, concepts: List[ConceptVariant]): TrainObject = {
    new TrainObject(records.map(_.lemma), state, concepts)
  }
}


case class ConceptsAnnotation(
                               val fromPos: Int,
                               val toPos: Int,
                               val concepts: List[ConceptVariant]
                             ) {}

class ExamplesBuilder {

  val searcher = Searcher

  val chunker: Chunker = LocalChunker

  val splitter: SentenceSplitter = LocalSplitter

  val parser: CoreNLPTools = LocalCoreNLP

  def convertWithoutAnnotations(records: List[ChunkRecord], listBuffer: ListBuffer[TrainObject]) = {
    val groups = records.groupBy(record => (record.parseTag, record.ner, record.groupId) )
    groups.toSeq.sortBy(x => x._1._3).foreach(group => {
      val ((parserTag, ner, _), groupRecords) = group
      val trainObject = Builder.makeTrainFromRecords(groupRecords, s"${parserTag}_$ner", Nil)
      listBuffer.append(trainObject)
    })
  }

  def convertRecords(records: List[ChunkRecord], annotations: List[ConceptsAnnotation], listBuffer: ListBuffer[TrainObject]): Unit = {
    annotations match {
      case head :: tail => {
        val (beforeAnnotation, withAnnotation) = records.span(record => record.endPos < head.fromPos)
        convertWithoutAnnotations(beforeAnnotation, listBuffer)
        val (annotated, afterAnnotation) = withAnnotation.span(record => record.beginPos < head.toPos)

        val trainObject = Builder.makeTrainFromRecords(annotated, s"${annotated.head.parseTag}_${annotated.head.ner}" , head.concepts)
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

    val sentenceRange = splitter.getSentence(text, (startMentionPos, endMentionPos)).get

    val sentence = text.substring(sentenceRange._1, sentenceRange._2)

    val annotations = List(ConceptsAnnotation(startMentionPos - sentenceRange._1, endMentionPos - sentenceRange._1, concepts))

    buildFromAnnotations(sentence, annotations)
  }

  def build(concept: String): List[List[TrainObject]] = {
    val searchRes = searcher.findHref(concept)
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
          val firstSents = chunker.chunkSentence(firstChunk.text)
          val lastSents = chunker.chunkSentence(lastChunk.text)

          val firstPart = if (firstSents.isEmpty) "" else firstSents.last
          val middlePart = middleChunk.text
          val lastPart = if (lastSents.isEmpty) "" else lastSents(0)

          FileLogger.logToFile("/tmp/learning.log", firstPart ++ " | " ++ middlePart ++ " | " ++ lastPart)

          chunker.group(List(firstPart, middlePart, lastPart)) match {
            case List(firstTags, middleTags, lastTags) => {
              Builder.makePosTrain(firstTags) ++
                List(new TrainObject(List(middlePart), "group", conceptVariant)) ++
                Builder.makePosTrain(lastTags)
            }
          }
        } /* end case */
      } /* end match */
    }) /* end map*/
  }

}
