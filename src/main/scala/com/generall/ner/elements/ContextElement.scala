package com.generall.ner.elements

import com.generall.sknn.model.storage.elements.BaseElement

/**
  * Created by generall on 14.08.16.
  */
class ContextElement(_context: List[BaseElement], element: BaseElement) extends BaseElement {
  val context = _context
  val mainElement = element
  override var label: String = element.label
  override var output: Set[String] = element.output
}

object ContextElementConverter {
  def convert[T <: BaseElement](orig: List[T], size: Int): List[ContextElement] = {
    assert(size % 2 == 1)
    (List.fill(size / 2)(NullElement) ++ orig ++ List.fill(size / 2)(NullElement)).sliding(size)
      .map(slide => new ContextElement(slide, slide(size / 2))).toList
  }

  def changeMainElement(contextElement: ContextElement, newElement: BaseElement): ContextElement = {
    val newContext = contextElement.context.map(oldElement => {
      if(oldElement == contextElement.mainElement)
        newElement
      else
        oldElement
    })
    new ContextElement(newContext, newElement)
  }

  def makeVariants(contextElement: ContextElement): List[ContextElement] = {
    contextElement.mainElement match {
      case multiElement: MultiElement[BaseElement] => multiElement.subElements.map(subElement => changeMainElement(contextElement, subElement))
      case el: BaseElement => List(contextElement)
    }
  }
}