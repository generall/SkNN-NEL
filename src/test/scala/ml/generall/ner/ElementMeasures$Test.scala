package com.generall.ner

import com.generall.ner.elements.MultiElement
import org.scalatest.FunSuite

/**
  * Created by generall on 14.08.16.
  */
class ElementMeasures$Test extends FunSuite {

  test("testBaseElementDistance1") {

  }

  test("testBaseElementDistance2") {

  }

  test("testWeightedJaccardDisatnce") {

    val map1 = new TestWeightedElement( Map("a" -> 0.1))
    val map2 = new TestWeightedElement( Map("a" -> 0.2))
    val map3 = new TestWeightedElement( Map("a" -> 0.19))
    val map4 = new TestWeightedElement( Map("a" -> 0.9))
    val map5 = new TestWeightedElement( Map("a" -> 0.3))

    val mEl1 = new MultiElement[TestWeightedElement]
    val mEl2 = new MultiElement[TestWeightedElement]

    mEl1.addElement(map1)
    mEl1.addElement(map2)
    mEl1.addElement(map3)

    mEl2.addElement(map4)
    mEl2.addElement(map5)

    assert((ElementMeasures.baseElementDistance(mEl1, mEl2) - 0.33333).abs < 0.001)

  }

}
