package ml.generall.ner

import ml.generall.ner.elements.{ContextElement, ContextElementConverter, MultiElement, OntologyElement}
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
      case head :: tail =>
        val resolved = head match {
          case contextElement: ContextElement =>
            contextElement.mainElement match {
              case multyElement: MultiElement[_] =>
                val variants = multyElement.subElements.filter { case _: OntologyElement => true; case _ => false }
                if (variants.size == 1) {
                  variants.head
                } else {
                  variants.maxBy { case el: OntologyElement => el.features.getOrElse(nodes.head.label, 0.0); case _ => 0.0 }
                }
            }
        }
        resolved :: recover(tail, nodes.head, nodes.tail)
      case Nil => Nil
    }
  }
}
