import scala.collection.mutable


val m = mutable.Map(1 -> 2)


m.getOrElseUpdate(2, 100)

m.contains(2)