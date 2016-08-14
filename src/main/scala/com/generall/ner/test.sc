

class Base{
}

class Der extends  Base{
}



val l1: List[Base] = List(new Base)
val l2: List[Base] = List(new Der)

val l3: List[Der] = List(new Der)

l1.isInstanceOf[List[Der]]
l1.isInstanceOf[List[Base]]

l3.isInstanceOf[List[Der]]
l3.isInstanceOf[List[Base]]


1.0 / 0.0 == Double.PositiveInfinity