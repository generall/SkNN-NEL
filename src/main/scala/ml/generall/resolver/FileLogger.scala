package ml.generall.resolver

import scala.reflect.io.File

/**
  * Created by generall on 04.09.16.
  */
object FileLogger {

  def logToFile(fname: String, str: String) = {
    File(fname).appendAll(str ++ "\n")
  }

}
