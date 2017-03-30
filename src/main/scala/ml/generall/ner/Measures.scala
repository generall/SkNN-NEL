package ml.generall.ner

import ml.generall.ner.elements._
import ml.generall.sknn.model.storage.elements.BaseElement

import scalaz._
import Scalaz._
import scalaz.outlaws.std.double._

/**
  * Created by generall on 13.08.16.
  */
object Measures {

  var count = 0

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

  def nonLinearWeightedJaccardDisatnce(x: Map[String, Double], y: Map[String, Double]): Double = {
    count += 1
    nonLinearTransform2(weightedJaccardDisatnce(x, y))
  }

}


object ElementMeasures {

  var count = 0

  type WMap = Map[String, Double]

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

  def bagOfWordsDistance( xMap: WMap, yMap: WMap, yList: List[BaseElement] )
                         (measureFunc: (WMap, WMap) => Double ): Double = {
    yList match {
      case Nil => measureFunc(xMap, yMap)
      case head :: tail => {
        head match {
          case contextElement: ContextElement => bagOfWordsDistance(xMap, yMap, contextElement.context ++ tail)(measureFunc)
          case NullElement =>  bagOfWordsDistance(xMap, yMap, tail)(measureFunc)
          case yWSet: WeightedSetElement =>  bagOfWordsDistance(xMap, yMap |+| yWSet.features, tail)(measureFunc)
          case yMulti: MultiElement[WeightedSetElement] => yMulti.subElements.map(yWSet => bagOfWordsDistance(xMap, yMap |+| yWSet.features, tail)(measureFunc)).min
        }
      }
    }
  }

  def bagOfWordsDistance(xMap: WMap, context: List[BaseElement], y: BaseElement)
                        (measureFunc: (WMap, WMap) => Double ): Double = {
    context match {
      case Nil => bagOfWordsDistance(xMap, Map[String, Double](), List(y))(measureFunc)
      case head :: tail => {
        head match {
          case xContextElement: ContextElement => bagOfWordsDistance(xMap, xContextElement.context ++ tail, y)(measureFunc)
          case NullElement => bagOfWordsDistance(xMap, tail, y)(measureFunc)
          case xWSet: WeightedSetElement => bagOfWordsDistance(xWSet.features |+| xMap, tail, y)(measureFunc)
          case xMulti: MultiElement[WeightedSetElement] => xMulti.subElements.map(xWSet => bagOfWordsDistance(xWSet.features |+| xMap, tail, y)(measureFunc)).min
        }
      }
    }
  }

  def baseElementDistance(x: BaseElement, y: BaseElement): Double = {
    weightedDistance(x, y)(weightedJaccardDisatnce)
  }

  def overlapElementDistance(x: BaseElement, y: BaseElement): Double = {
    weightedDistance(x, y)(weightedOverlap)
  }

  def bagOfWordElementDistance(x: BaseElement, y: BaseElement): Double = {
    count += 1
    bagOfWordsDistance(Map[String, Double](), List(x), y)(Measures.nonLinearWeightedJaccardDisatnce)
  }

}