package ml.generall.http

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http._
import StatusCodes._
import akka.actor.ActorSystem


class MyServiceSpec extends Specification with Specs2RouteTest with MyService {
  def actorRefFactory: ActorSystem = system
  
  "MyService" should {

    "return a greeting for GET requests to the root path" in {
      Get() ~> myRoute ~> check {
        responseAs[String] must contain("Say hello")
      }
    }

    "leave GET requests to other paths unhandled" in {
      Get("/kermit") ~> myRoute ~> check {
        handled must beFalse
      }
    }

  }
}
