package ml.generall.ner.elements

/**
  * Created by generall on 13.08.16.
  */
class POSElement( tag: POSTag ,_label: String = null, singleOutput: String = null) extends WeightedSetElement {


  override def features: Map[String, Double] = Map(tag.word -> 1.0, tag.tag -> 1.0)

  override var label: String = if(_label == null) tag.tag else _label
  override var output: Set[String] = if(singleOutput == null) Set(tag.word, tag.tag) else Set(singleOutput)

}
