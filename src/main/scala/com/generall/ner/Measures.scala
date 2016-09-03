package com.generall.ner

import com.generall.ner.elements._
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
    if (x.isEmpty && y.isEmpty)
      return 0.0 // prevent 0 by 0 division, 2 empty sets actually equals to each other
    val intersection = weightedIntersection(x, y)
    val xSum = x.foldLeft(0.0)((sum, x) => sum + x._2)
    val ySum = y.foldLeft(0.0)((sum, x) => sum + x._2)

    1 - intersection / (xSum + ySum - intersection)
  }


  def nonLinearTransform1(y: Double): Double = 1 / (1 - y) - 1

  def nonLinearTransform2(y: Double): Double = Math.pow(y, 2) // TODO: hyper parameter

}


object ElementMeasures {
  def weightedJaccardDisatnce(x: WeightedSetElement, y: WeightedSetElement): Double = {
    val res = Measures.weightedJaccardDisatnce(x.features, y.features)
    Measures.nonLinearTransform1(res)
  }

  def weightedOverlap(x: WeightedSetElement, y: WeightedSetElement): Double  = {
    val res = Measures.weightedJaccardDisatnce(x.features, y.features)
    Measures.nonLinearTransform2(res)
  }

  def weightedDistance(x: WeightedSetElement, y: BaseElement)(measureFunc: (WeightedSetElement, WeightedSetElement) => Double): Double = {
    y match {
      case contextElement: ContextElement =>  throw new RuntimeException("unable to match context with non-context")
      case NullElement => measureFunc(x, EmptyWeightedElement)
      case yWSet: WeightedSetElement => measureFunc(x, yWSet)
      case yMulti: MultiElement[WeightedSetElement] => yMulti.subElements.map(yWSet => measureFunc(x, yWSet)).min
    }
  }


  def weightedDistance(x: BaseElement, y: BaseElement)(measureFunc: (WeightedSetElement, WeightedSetElement) => Double): Double = {
    x match {
      case xContextElement: ContextElement => y match {
        case yContextElement: ContextElement => {
          assert(yContextElement.context.size == xContextElement.context.size)
          yContextElement.context.zip(xContextElement.context)
            .foldLeft(0.0)((sum, pair) => sum + weightedDistance(pair._1, pair._2)(measureFunc) )
        }
        case _ => throw new RuntimeException("unable to match context with non-context")
      }
      case NullElement => weightedDistance(EmptyWeightedElement, y)(measureFunc)
      case xWSet: WeightedSetElement => weightedDistance(xWSet, y)(measureFunc)
      case xMulti: MultiElement[WeightedSetElement] => xMulti.subElements.map(xWSet => weightedDistance(xWSet, y)(measureFunc)).min
    }
  }

  def baseElementDistance(x: BaseElement, y: BaseElement): Double = {
    weightedDistance(x, y)(weightedJaccardDisatnce)
  }

  def overlapElementDistance(x: BaseElement, y: BaseElement): Double = {
    weightedDistance(x, y)(weightedOverlap)
  }

}