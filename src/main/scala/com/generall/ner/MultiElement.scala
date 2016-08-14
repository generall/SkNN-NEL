package com.generall.ner

import com.generall.sknn.model.storage.elements.BaseElement

/**
  * Created by generall on 14.08.16.
  */
class MultiElement[T <: BaseElement] extends BaseElement{

  var subElements: List[T] = Nil

  def addElement(element: T) = {
    subElements = element :: subElements
  }

  override var label: String = null
  override var output: Set[String] = Set()
}
