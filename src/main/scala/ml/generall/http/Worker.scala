package ml.generall.http

import ml.generall.resolver.SentenceAnalizer
import ml.generall.resolver.dto.ConceptsAnnotation

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by generall on 24.09.16.
  */
object Worker {
  val analyzer = new SentenceAnalizer

  def analyse(sentence: String): List[ConceptsAnnotation] = {
    val f = Future({
      analyzer.analyse(sentence)
    })
    Await.result(f, 5.minutes)
  }

}
