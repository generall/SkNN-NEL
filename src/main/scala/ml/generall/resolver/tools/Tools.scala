package ml.generall.resolver.tools

import com.typesafe.scalalogging.Logger

/**
  * Created by generall on 19.03.17.
  */
object Tools {

  val logger = Logger("time")

  def time[R](block: => R, msg: String = ""): R = {
    val t0 = System.currentTimeMillis()
    val result = block    // call-by-name
    val t1 = System.currentTimeMillis()
    logger.info("Elapsed time: " + (t1 - t0) + s"ms $msg" )
    result
  }
}
