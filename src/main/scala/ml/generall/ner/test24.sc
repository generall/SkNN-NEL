import scala.collection.mutable

val map1 = Map(1 -> 9 , 2 -> 20)

val map2 = Map(1 -> 100, 3 -> 300)

map1 ++ map2.map{ case (k,v) => k -> (v + map1.getOrElse(k,0)) }


val m = mutable.Map(1 -> 2)

m(1) += 4

m