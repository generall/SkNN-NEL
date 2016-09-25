package ml.generall.ner.elements

import ml.generall.sknn.model.storage.elements.BaseElement

/**
  * Created by generall on 14.08.16.
  */
object NullElement extends BaseElement{
  override var label: String = null
  override var output: Set[String] = null
}
