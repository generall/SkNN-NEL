package ml.generall.http

import ml.generall.resolver.SentenceAnalizer

/**
  * Created by generall on 24.09.16.
  */
object SentenceConverter {
  val analizer = new SentenceAnalizer

  def convert(sentence: String): ParseResult = {
    ParseResult(sentence, analizer.prepareSentence(sentence))
  }

}
