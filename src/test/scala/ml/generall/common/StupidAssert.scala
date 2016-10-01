package ml.generall.common

/**
  * Created by generall on 02.10.16.
  */
object StupidAssert {
  def assert(value: Boolean) = {
    if(!value) throw new RuntimeException("Assert failed")
  }
}
