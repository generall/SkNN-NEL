package ml.generall.resolver.tools

/**
  * Created by generall on 19.03.17.
  */
object Tools {
  def time[R](block: => R, msg: String = ""): R = {
    val t0 = System.currentTimeMillis()
    val result = block    // call-by-name
    val t1 = System.currentTimeMillis()
    println("Elapsed time: " + (t1 - t0) + s"ms $msg" )
    result
  }
}
