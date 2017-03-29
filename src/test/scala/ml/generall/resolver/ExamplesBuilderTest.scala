package ml.generall.resolver

import ml.generall.elastic.Chunk
import ml.generall.resolver.dto.{ConceptVariant, ConceptsAnnotation}
import ml.generall.resolver.tools.{SaveTools, Tools}
import org.scalatest.{BeforeAndAfterEach, FunSuite}

/**
  * Created by generall on 27.08.16.
  */
class ExamplesBuilderTest extends FunSuite with BeforeAndAfterEach {

  var builder = new ExamplesBuilder()

  override def beforeEach() {
  }

  test("testBuild") {
    val res = builder.build("http://en.wikipedia.org/wiki/Batman")
    res.foreach(seq => {
      seq.foreach(_.print())
      println()
      println(" ================================= ")
      println()
    })
  }

  test("customExtendMentionSearch") {
    val mentionResult = Builder.searchMention("batman")
    mentionResult.stats.foreach(println)

  }


  test("testCreateMockups") {
    val exampleBuilder = new ExamplesBuilder

    val urls = List(
      "http://en.wikipedia.org/wiki/Titanic_(1997_film)",
      "http://en.wikipedia.org/wiki/RMS_Titanic",
      "http://en.wikipedia.org/wiki/Titanic",
      "http://en.wikipedia.org/wiki/Iceberg_Theory",
      "http://en.wikipedia.org/wiki/Iceberg_(fashion_house)",
      "http://en.wikipedia.org/wiki/Iceberg",
      "http://en.wikipedia.org/wiki/James_Cameron"
    )


    val trains: BuilderMockup.HrefStore = Tools.time{
      urls.par.map(href =>
        href -> exampleBuilder.builder.searchMentionsByHref(href).toList
      ).toList
    }

    val mentions = List(
      "Titanic",
      "iceberg",
      "James Cameron"
    )

    val mentionsTrain: BuilderMockup.MentionStore = Tools.time {
      mentions.par.map(m =>
        m -> exampleBuilder.builder.searchMention(m)
      ).toList
    }

    SaveTools.save("/tmp/href_mockup", trains)
    SaveTools.save("/tmp/mention_mockup", mentionsTrain)
  }

  test("testReadMockups") {
    println(BuilderMockup.getClass.getResource("/href_mockup").getFile)

    val mention = BuilderMockup.searchMention("Titanic")
    val iceberg = BuilderMockup.searchMention("iceberg")
    mention.stats.foreach(println)
    println("-----------")
    iceberg.stats.foreach(println)
    println("===========")


    val titanicSentences = BuilderMockup.searchMentionsByHref("http://en.wikipedia.org/wiki/RMS_Titanic")
    val icebergSentences = BuilderMockup.searchMentionsByHref("http://en.wikipedia.org/wiki/Iceberg_Theory")

    titanicSentences.foreach(x => println(x.sent))
    println("-----------")
    icebergSentences.foreach(x => println(x.sent))
  }
}
