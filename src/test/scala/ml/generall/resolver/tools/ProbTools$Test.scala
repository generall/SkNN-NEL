package ml.generall.resolver.tools

import org.scalatest.FunSuite

/**
  * Created by generall on 24.08.16.
  */
class ProbTools$Test extends FunSuite {

  test("testSoftMaxNormalizationCoef") {


  }

  test("testNormalizationCoef") {

  }

  test("testSoftMax") {


    val list = List(1, 2, 3, 4)

    val normList = ProbTools.softMax(list)

    assert(normList.max < 1.0)

    println(normList)

  }

  test("testNormalize") {

    val list = List(1, 2, 3, 4)

    val normList = ProbTools.normalize(list)

    assert((normList.sum  - 1.0).abs < 0.001)

    println(normList)

  }

}
