package ml.generall.ner.elements

import ml.generall.sknn.model.storage.elements.BaseElement

/**
  * Created by generall on 14.08.16.
  */
class MultiElement[T <: BaseElement] extends BaseElement{

  var subElements: List[T] = Nil

  def addElement(element: T) = {
    subElements = element :: subElements
  }

  def genLabel = subElements.map(_.label).sorted.mkString("_")

  override var label: String = ""
  override var output: Set[String] = Set()
}
