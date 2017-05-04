package ml.generall.resolver.tools

import com.typesafe.scalalogging.Logger

/**
  * Created by generall on 19.03.17.
  */
object Tools {

  val logger = Logger("time")

  def time[R](block: => R, msg: String = ""): R = {
    val t0 = System.currentTimeMillis()
    val result = block // call-by-name
    val t1 = System.currentTimeMillis()
    val elapsed = t1 - t0
    if (elapsed > 500) {
      logger.info(s"Elapsed time: ${elapsed}ms $msg")
    }
    result
  }
}
