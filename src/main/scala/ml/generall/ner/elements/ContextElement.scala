package ml.generall.ner.elements

import ml.generall.sknn.model.storage.elements.BaseElement

import scala.collection.AbstractIterator

/**
  * Created by generall on 14.08.16.
  */
class ContextElement(_context: List[BaseElement], element: BaseElement) extends BaseElement with Iterable[BaseElement] {
  val context: List[BaseElement] = _context
  val mainElement: BaseElement = element
  override var label: String = element.label
  override var output: Set[String] = element.output

  override def iterator: Iterator[BaseElement] = mainElement match {
    case x: MultiElement[_] => x.iterator
    case x: BaseElement => List(x).iterator

  }
}

object ContextElementConverter {

  def convertContext[T >: Null](orig: List[T], size: Int): Iterator[(List[T], T, List[T])] = {
    assert(size % 2 == 1)
    val contextSize = size / 2
    (List.fill[T](contextSize)(null) ++ orig ++ List.fill[T](contextSize)(null)).sliding(size).map(slide => {
      val (leftContext, other) = slide.splitAt(contextSize)
      val (elem, rightContext) = other.splitAt(1)
      (leftContext.filter(_ != null), elem.head, rightContext.filter(_ != null))
    })
  }

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
      case multiElement: MultiElement[_] => multiElement.subElements.map(subElement => changeMainElement(contextElement, subElement))
      case _: BaseElement => List(contextElement)
    }
  }
}