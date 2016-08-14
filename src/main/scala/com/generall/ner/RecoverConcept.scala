package com.generall.ner

import com.generall.sknn.model.SkNNNode
import com.generall.sknn.model.storage.elements.BaseElement

/**
  * Created by generall on 14.08.16.
  */
object RecoverConcept {

  def recover(seq: List[BaseElement], currentNode: SkNNNode[BaseElement], nodes: List[SkNNNode[BaseElement]] ): List[OntologyElement] = {
    seq match {
      case head :: tail => head match {
        case ontologyElement: OntologyElement  => ontologyElement :: recover(tail, nodes.head, nodes.tail )
        case multiElement: MultiElement[OntologyElement] =>
          multiElement.subElements.minBy(el => currentNode.calcDistance(el, nodes.head)):: recover(tail, nodes.head, nodes.tail )
      }
      case Nil => Nil
    }
  }
}
