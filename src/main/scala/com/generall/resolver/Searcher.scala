package com.generall.resolver

import ml.generall.elastic.{MentionSearchResult, MentionSearcher}
import ml.generall.nlp.{IDFDict, CoreNLPTools, SentenceSplitter, OpenNLPChunker}

/**
  * Created by generall on 27.08.16.
  */
object Searcher extends MentionSearcher( /*"192.168.1.44" */ "localhost" , 9300, "wiki") {


  override def findHref(href: String) = {
    println(s"Finding href: $href")
    val res = super.findHref(href)
    println(s"Found: ${res.size}")
    res
  }

  override def findMentions(mentionText: String): MentionSearchResult = {
    println(s"Finding mention: $mentionText")
    val res = super.findMentions(mentionText)
    println(s"Found: ${res.stats.size}")
    res
  }


}

/*
    Make objects singletons
 */

object LocalIDFDict extends IDFDict {}

object LocalSplitter extends SentenceSplitter {}

object LocalCoreNLP extends CoreNLPTools {}