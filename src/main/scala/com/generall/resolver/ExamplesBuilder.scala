package com.generall.resolver

import ml.generall.elastic.ConceptVariant
import ml.generall.nlp.Chunker

/**
  * Created by generall on 27.08.16.
  */

object Builder{

  val searcher = Searcher

  def makeTrain(groups: List[(String, List[(String, (String, String))])]): List[TrainObject] = {
    groups.flatMap({ case (group, list) => {
      group match {
        case "NP" => {
          val text = list.map(_._1).mkString(" ")
          val variants = searcher.findMentions(text).stats
          List(new TrainObject(text, "group", group, variants))
        }
        case _ =>
          list.map(word => {
            val (token, (pos, chunkTag)) = word
            new TrainObject(token, pos, chunkTag, Nil)
          })
      }
    }})
  }
}

class ExamplesBuilder {

  val searcher = Searcher

  val chunker: Chunker = LocalChunker

  def build(concept: String): List[List[TrainObject]] = {
    val searchRes = searcher.findHref(concept)
    val count = searchRes.size

    val conceptVariant = List(ConceptVariant(
      concept = concept,
      count = count,
      avgScore = 1.0,
      avgNorm = 1.0,
      avgSoftMax = 1.0,
      maxScore = 1.0,
      minScore = 1.0
    ))
    searchRes.map(item => {
      item.chunks match {
        case List(firstChunk, middleChunk, lastChunk) => {
          val firstSents = chunker.chunkSentence(firstChunk.text)
          val lastSents = chunker.chunkSentence(lastChunk.text)

          val firstPart = if (firstSents.isEmpty) "" else firstSents.last
          val middlePart = middleChunk.text
          val lastPart = if (lastSents.isEmpty) "" else lastSents(0)

          chunker.group(List(firstPart, middlePart, lastPart)) match {
            case List(firstTags, middleTags, lastTags) => {
              Builder.makeTrain(firstTags) ++
                List(new TrainObject(middlePart, "group", middleTags.head._1, conceptVariant)) ++
                Builder.makeTrain(lastTags)
            }
          }
        }
      }
    })
  }

}
