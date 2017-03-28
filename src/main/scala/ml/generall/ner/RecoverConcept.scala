package ml.generall.ner

import ml.generall.ner.elements.{ContextElementConverter, ContextElement, MultiElement}
import ml.generall.sknn.model.SkNNNode
import ml.generall.sknn.model.storage.elements.{BaseElement, SetElement}

/**
  * Created by generall on 14.08.16.
  */
object RecoverConcept {

  def matchElement(element: BaseElement, node: SkNNNode[BaseElement], nextNode: SkNNNode[BaseElement]): BaseElement = {

    element match {
      case setElement: SetElement => setElement
      case multiElement: MultiElement[_] =>
        multiElement.subElements.minBy(el => node.calcDistance(el, nextNode))
    }
  }

  def recover(seq: List[BaseElement], currentNode: SkNNNode[BaseElement], nodes: List[SkNNNode[BaseElement]]): List[BaseElement] = {
    seq match {
      case head :: tail => head match {
        case contextElement: ContextElement =>
          val variants = ContextElementConverter.makeVariants(contextElement)
          (if (variants.size == 1)
            contextElement
          else
            variants.minBy(contextEl => currentNode.calcDistance(contextEl, nodes.head))
            ) :: recover(tail, nodes.head, nodes.tail)
        case baseElement: BaseElement =>
          matchElement(baseElement, currentNode, nodes.head) :: recover(tail, nodes.head, nodes.tail)
      }
      case Nil => Nil
    }
  }
}
