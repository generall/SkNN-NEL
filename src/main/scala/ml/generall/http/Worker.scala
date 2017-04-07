package ml.generall.http

import ml.generall.resolver.SentenceAnalizer
import ml.generall.resolver.dto.ConceptsAnnotation

import scala.concurrent.Future
import scala.concurrent.duration._


/**
  * Created by generall on 24.09.16.
  */
object Worker {
  val analyzer = new SentenceAnalizer

  def analyse(sentence: String): List[ConceptsAnnotation] = Future { analyzer.analyse(sentence) }.result(2.minutes)

}
