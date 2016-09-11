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


  test("testTitanic") {

    val analizer = new SentenceAnalizer


    analizer.analyse("James Cameron made Titanic")

    analizer.analyse("Titanic struck Iceberg")

  }

  test("testTeresa") {

    val str = "When Mother Teresa received the Nobel Peace Price, she used the opportunity of her worldwide telecast speech in Oslo to declare abortion the greatest evil in the world and to launch a fiery call against population control."

    val analizer = new SentenceAnalizer

    analizer.analyse(str)

  }

}
