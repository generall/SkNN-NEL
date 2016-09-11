package com.generall.resolver

import ml.generall.elastic.ConceptVariant

/**
  * Created by generall on 27.08.16.
  */
class TrainObject(_text:Iterable[String], _pos: String, _chunkTag: String, _concepts: Iterable[ConceptVariant]) {

  var concepts = _concepts

  val tokens = _text

  val pos = _pos

  val chunkTag = _chunkTag

  def print() = {
    println(s"----- $tokens ----")
    println(s"Pos: $pos")
    println(s"chunkTag: $chunkTag")
    concepts.foreach(concept => {
      println(s"\t${concept.concept} ${concept.count}")
    })
  }

}
