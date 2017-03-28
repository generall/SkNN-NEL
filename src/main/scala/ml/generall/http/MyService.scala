package ml.generall.http

import akka.actor.Actor
import ml.generall.resolver.TrainObject
import spray.routing._
import spray.http._
import spray.json._
import spray.httpx.SprayJsonSupport._
import MediaTypes._


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

case class TestStruct(n: Int, s: List[String]){}
object TestStructProtocol extends DefaultJsonProtocol {
  implicit val data = jsonFormat2(TestStruct)
}

case class EntityParams(
                         avgWeight: Double,
                         maxWeight: Double,
                         wordCount: Int,
                         vote: String
                       ){}

object Storage{
  var list: List[EntityParams] = Nil
}

// this trait defines our service behavior independently from the service actor
trait MyService extends HttpService {

  import TestStructProtocol._

  val myRoute: Route =
    path("1") {
      get {
        complete(TestStruct(1, List("test")).toJson.prettyPrint)
      }
    } ~ path("3") {
      post{
        entity(as[TestStruct]) {
          body => {
            println(body)
            complete("ok")
          }
        }
      }
    }  ~ pathPrefix("public") {
      getFromDirectory("public")
    }
}