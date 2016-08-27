package com.generall.resolver

import ml.generall.elastic.MentionSearcher
import ml.generall.nlp.OpenNLPChunker

/**
  * Created by generall on 27.08.16.
  */
object Searcher extends MentionSearcher("localhost", 9300){

  /*
  override def findHref(href: String) = {
    println(s"Finding href: $href")
    super.findHref(href)
  }

  override def findMentions(mentionText: String): MentionSearchResult = {
    println(s"Finding mention: $mentionText")
    super.findMentions(mentionText)
  }
  */

}

object LocalChunker extends OpenNLPChunker{}