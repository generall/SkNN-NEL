
import ml.generall.resolver.tools.Tools

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

val a = List(1, 2, 3, 4)

val futures = a.map(x => Future {
  Thread.sleep(x * 2000)
  println(s"Sleeped for $x")
  x + 1
}.map(x => List(0, x + 100)))


val seqFuture = Future.sequence(futures).map(x => println(x.flatten))

Tools.time({ Await.ready(seqFuture, Duration.Inf) }, "await")

seqFuture.map(x => println(x))