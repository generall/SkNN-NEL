package ml.generall.resolver

/**
  * Created by generall on 17.09.16.
  */
case class UnparsableException(
                                val text: String,
                                val startMentionPos: Int,
                                val endMentionPos: Int
                              ) extends Throwable{

  override def toString() = s"Error: ${(text, startMentionPos, endMentionPos)}"

}
