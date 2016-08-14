package com.generall.ner

import com.generall.sknn.model.storage.elements.BaseElement

/**
  * Created by generall on 13.08.16.
  */
object Measures {

  def weightedIntersection(x: Map[String, Double], y: Map[String, Double]): Double = {
    val intersection = x.keySet.intersect(y.keySet)
    intersection.foldLeft(0.0)((sum, key) => sum + Math.min(x(key), y(key)))
  }

  def weightedJaccardDisatnce(x: Map[String, Double], y: Map[String, Double]): Double = {

    val intersection = weightedIntersection(x, y)
    val xSum = x.foldLeft(0.0)((sum, x) => sum + x._2)
    val ySum = y.foldLeft(0.0)((sum, x) => sum + x._2)

    1 - intersection / (xSum + ySum - intersection)
  }

  def nonLinearTransform(y: Double): Double = 1 / (1 - y) - 1

}


object ElementMeasures {
  def weightedJaccardDisatnce(x: WeightedSetElement, y: WeightedSetElement): Double = {
    val res = Measures.weightedJaccardDisatnce(x.features, y.features)
    Measures.nonLinearTransform(res)
  }

  def baseElementDistance(x: WeightedSetElement, y: BaseElement): Double = {
    y match {
      case yWSet: WeightedSetElement => weightedJaccardDisatnce(x, yWSet)
      case yMulti: MultiElement[WeightedSetElement] => yMulti.subElements.map(yWSet => weightedJaccardDisatnce(x, yWSet)).min
    }
  }

  def baseElementDistance(x: BaseElement, y: BaseElement): Double = {
    x match {
      case xWSet: WeightedSetElement => baseElementDistance(xWSet, y)
      case xMulti: MultiElement[WeightedSetElement] => xMulti.subElements.map(xWSet => baseElementDistance(xWSet, y)).min
    }
  }

}