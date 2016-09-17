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

  test("testNewParser") {
    val str = "I will go to London with Stalin on next weekend"

    val contextSize = 5

    val parser = LocalCoreNLP

    val groups = parser.process(str)
      .groupBy(record => (record.parseTag, record.ner, record.groupId))
      .toList
      .sortBy(x => x._1._3)
      .map(pair => (s"${pair._1._1}_${pair._1._2}", pair._2)) // creation of state

    val objs = Builder.makeTrain(groups)

    objs.foreach(_.print())

    /**
      * All concepts with disambiguation
      */
    val conceptsToLearn: List[(String, String, String)] = SentenceAnalizer.getConceptsToLearn(objs, contextSize)


    println("conceptsToLearn: ")
    conceptsToLearn.foreach(println)
  }

}
