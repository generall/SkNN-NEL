package com.generall.resolver

import ml.generall.elastic.ConceptVariant
import ml.generall.nlp.Chunker

/**
  * Created by generall on 27.08.16.
  */

object Builder {

  val searcher = Searcher


  /**
    * Make list of train objects from grouped chunks
    * @param groups groups of chunks
    * @return
    */
  def makeTrain(groups: List[(String, List[(String, (String, String))])]): List[TrainObject] = {
    groups.flatMap({ case (group, list) => {
      group match {
        case "NP" => {
          val text = list.map(_._1).mkString(" ")
          val variants = searcher.findMentions(text).stats
          List(new TrainObject(list.map(_._1), "group", group, variants))
        }
        case _ =>
          list.map(word => {
            val (token, (pos, chunkTag)) = word
            new TrainObject(List(token), pos, chunkTag, Nil)
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
          List(new TrainObject(text, "group", group, Nil))
        }
        case _ =>
          list.map(word => {
            val (token, (pos, chunkTag)) = word
            new TrainObject(List(token), pos, chunkTag, Nil)
          })
      }
    }
    })
  }
}

class ExamplesBuilder {

  val searcher = Searcher

  val chunker: Chunker = LocalChunker

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
    searchRes.filter(item => { /* filtering results with empty context */
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
                List(new TrainObject(List(middlePart), "group", middleTags.head._1, conceptVariant)) ++
                Builder.makePosTrain(lastTags)
            }
          }
        } /* end case */
      } /* end match */
    }) /* end map*/
  }

}
