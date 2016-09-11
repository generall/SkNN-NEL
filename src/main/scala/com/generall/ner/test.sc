import scalaz._
import Scalaz._
import scalaz.outlaws.std.double._

val ms = List(Map("hello" -> 1.1, "world" -> 2.2), Map("goodbye" -> 3.3, "hello" -> 4.4))
ms.reduceLeft(_ |+| _)