package com.generall.resolver

import org.scalatest.FunSuite

/**
  * Created by generall on 27.08.16.
  */
class SentenceAnalizerTest extends FunSuite {

  test("testAnalyse") {
    val analizer = new SentenceAnalizer

    analizer.analyse("I will go to London with Stalin on next weekend")
  }

}
