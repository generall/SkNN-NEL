package ml.generall.http

import akka.actor.Actor
import ml.generall.resolver.TrainObject
import spray.routing._
import spray.http._
import spray.json._
import spray.httpx.SprayJsonSupport._
import MediaTypes._
import ml.generall.resolver.dto.{ConceptDescription, ConceptsAnnotation}
import spray.http.HttpHeaders.RawHeader


// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor extends Actor with MyService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)
}

case class Sentence(s: String)

case class Result(annotations: List[ConceptsAnnotation])

object SentenceProtocol extends DefaultJsonProtocol {
  implicit val data = jsonFormat1(Sentence)
}

object NERProtocol extends DefaultJsonProtocol {
  implicit val description = jsonFormat2(ConceptDescription)
  implicit val annotation = jsonFormat3(ConceptsAnnotation)
  implicit val result = jsonFormat1(Result)
}


// this trait defines our service behavior independently from the service actor
trait MyService extends HttpService {

  import SentenceProtocol._
  import NERProtocol._

  val myRoute: Route = respondWithHeaders(
    RawHeader("Access-Control-Allow-Origin", "*"),
    RawHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept")
  ) {
    path("1") {
      get {
        complete(Sentence("test").toJson.prettyPrint)
      }
    } ~ path("analyze") {
      post {
        entity(as[Sentence]) {
          body => {
            println(body)
            val res = Result(annotations = Worker.analyse(body.s))
            complete(res)
          }
        }
      } ~
        options {
          complete("This is an OPTIONS request.")
        }
    } ~ pathPrefix("public") {
      getFromDirectory("./public")
    }
  }
}