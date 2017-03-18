package ml.generall.resolver.tools

import java.io._

/**
  * Functions for saving objects and restoring them back
  * Created by generall on 18.03.17.
  */
object SaveTools {
  def save[T <: Serializable](fileName: String, serializable: T): Unit = {
    val baos = new FileOutputStream(fileName)
    val oos = new ObjectOutputStream(baos)
    oos.writeObject(serializable)
    oos.close()
  }

  def load[T](fileName: String): T = {
    val ois = new ObjectInputStream(new FileInputStream(fileName))
    val res = ois.readObject.asInstanceOf[T]
    ois.close()
    res
  }

  def load[T](stream: InputStream): T = {
    val ois = new ObjectInputStream(stream)
    val res = ois.readObject.asInstanceOf[T]
    ois.close()
    res
  }
}
