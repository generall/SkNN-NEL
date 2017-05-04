package ml.generall.resolver.filters

import java.io.{FileInputStream, ObjectInputStream}

import libsvm.LibSVM

import ml.generall.nlp.ChunkRecord
import net.sf.javaml.core.DenseInstance

/**
  * Created by generall on 01.10.16.
  */
object SvmFilter extends MentionFilter {

  val ois = new ObjectInputStream(getClass.getResourceAsStream("/svm.data"))

  val svm = ois.readObject.asInstanceOf[LibSVM]

  val nerFilter = Set("TIME", "DATE")
  val posFilter = Set("CC", ",")

  override def filter(records: List[ChunkRecord], weights: List[Double]): Boolean = {
    if (records.forall(record => posFilter.contains(record.pos)))
      return false
    if (records.exists(token => nerFilter.contains(token.ner)))
      return false
    svm.classify(new DenseInstance(Array(weights.sum / records.size, weights.max, records.size))) == "up"
  }

}
