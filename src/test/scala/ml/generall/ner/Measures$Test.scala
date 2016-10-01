package ml.generall.ner

import ml.generall.common.StupidAssert
import org.scalatest.FunSuite

/**
  * Created by generall on 13.08.16.
  */
class Measures$Test extends FunSuite {

  test("testWeightedIntersection") {


    val list1 = List(
      ("a", 0.1),
      ("b", 0.2),
      ("c", 0.1),
      ("e", 0.5)
    ).toMap

    val list2 = List(
      ("a", 0.2),
      ("b", 0.7),
      ("d", 0.7),
      ("e", 0.4)
    ).toMap

    StupidAssert.assert((Measures.weightedIntersection(list1, list2) - 0.7).abs < 0.00001)
    StupidAssert.assert((Measures.weightedIntersection(list2, list1) - 0.7).abs < 0.00001)
  }

  test("testWeightedJaccardIndex") {
    val list1 = List(
      ("a", 0.1),
      ("b", 0.2),
      ("c", 0.1),
      ("e", 0.5)
    ).toMap

    val list2 = List(
      ("a", 0.2),
      ("b", 0.7),
      ("d", 0.7),
      ("e", 0.4)
    ).toMap

    StupidAssert.assert( (Measures.weightedJaccardDisatnce(list1, list2) - (1 - 0.7/2.2)).abs < 0.0001 )

  }

}
