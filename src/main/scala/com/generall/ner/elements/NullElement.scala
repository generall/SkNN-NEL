package com.generall.ner.elements

import com.generall.sknn.model.storage.elements.BaseElement

/**
  * Created by generall on 14.08.16.
  */
object NullElement extends BaseElement{
  override var label: String = null
  override var output: Set[String] = null
}
