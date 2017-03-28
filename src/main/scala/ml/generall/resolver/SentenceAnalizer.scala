package ml.generall.resolver

import ml.generall.ner.elements.{ContextElement, _}
import ml.generall.ner.{ElementMeasures, RecoverConcept}
import ml.generall.resolver.dto.ConceptVariant
import ml.generall.resolver.tools.Tools
import ml.generall.sknn.SkNN
import ml.generall.sknn.model.storage.PlainAverageStorage
import ml.generall.sknn.model.storage.elements.BaseElement
import ml.generall.sknn.model.{Model, SkNNNode, SkNNNodeImpl}

import scala.collection.mutable


object SentenceAnalizer {


  def getConceptsToLearn(objList: List[TrainObject], contextSize: Int): List[(String, String, String)] = {

    var res: List[(String, String, String)] = Nil
    val context = ContextElementConverter.convertContext(objList, contextSize).toList
    context.foreach({
      case (leftContext, elem, rightContext) =>
        if (elem.concepts.size > 1) {
          val leftContextString = leftContext.flatMap(elem => elem.tokens.map(_._1)).mkString(" ")
          val rightContextString = rightContext.flatMap(elem => elem.tokens.map(_._1)).mkString(" ")
          elem.concepts.foreach(concept => res = (leftContextString, concept.concept, rightContextString) :: res)
        }
    })
    res
  }

  def wikiToDbpedia(wikilink: String): String = {
    wikilink.replaceAllLiterally("en.wikipedia.org/wiki", "dbpedia.org/resource")
  }

  def toBagOfWordsElement(obj: TrainObject): BaseElement = {
    val element = new BagOfWordElement(obj.tokens.map(lemma => {
      (lemma._1, lemma._2)
    }).toMap, obj.state)
    obj.concepts match {
      case Nil => element
      case List(concept) => {
        val multi = new MultiElement[WeightedSetElement]
        val onto = new OntologyElement(SentenceAnalizer.wikiToDbpedia(concept.concept), conceptWeight = concept.getWeight)
        if (onto.features.nonEmpty)
          multi.addElement(onto)
        multi.addElement(element)
        multi.label = obj.state // multi.genLabel
        multi
      }
      case disambiguation: Iterable[ConceptVariant] => {
        val multi = new MultiElement[WeightedSetElement]

        disambiguation
          .view
          .map(x => new OntologyElement(SentenceAnalizer.wikiToDbpedia(x.concept), conceptWeight = x.getWeight))
          .filter(element => element.features.nonEmpty)
          .foreach(multi.addElement)

        multi.addElement(element)
        multi.label = obj.state
        multi
      }
    }
  }

}

/**
  * Created by generall on 27.08.16.
  */
class SentenceAnalizer {


  val contextSize = 5
  val searcher = Searcher
  val parser = LocalCoreNLP
  val exampleBuilder = new ExamplesBuilder

  def prepareSentence(sentence: String): List[TrainObject] = {
    val parseRes = Tools.time(parser.process(sentence), "parser")

    val groups = parseRes.zipWithIndex
      .groupBy({ case (record, _) => (record.parseTag, 0 /*record.ner*/ , record.groupId) })
      .toList
      .sortBy(x => x._2.head._2)
      .map(pair => (s"${pair._1._1}" /* _${pair._1._2} */ , pair._2.map(_._1))) // creation of state

    exampleBuilder.makeTrain(groups)
  }


  /**
    * Filter predicate, keep only OntologyElements
    *
    * @return keep element?
    */
  def filterSequencePredicate(el: ContextElement): Boolean = el.mainElement match {
    case x: MultiElement[_] => x.subElements.exists {
      case y: OntologyElement => y.nonEmpty
      case _ => false
    }
    case y: OntologyElement => y.nonEmpty
    case _ => false
  }


  def filterSequence(seq: List[ContextElement]): List[ContextElement] = seq.filter(filterSequencePredicate)

  /**
    * Prepare training set for disambiguation
    */
  def getTrainingSet(conceptsToLearn: List[(String, String, String)]): List[List[ContextElement]] = conceptsToLearn
    .par
    .flatMap(x => exampleBuilder.build(x._2, x._1, x._3))
    .filter(_.nonEmpty)
    .map(convertToContext)
    .toList


  def convertToContext(objects: List[TrainObject]): List[ContextElement] = filterSequence(
    ContextElementConverter.convert(objects.map(SentenceAnalizer.toBagOfWordsElement), contextSize))


  def getAllWeightedCategories(trainSet: List[List[ContextElement]]): mutable.Map[String, Double] = trainSet.flatMap(_.flatMap(x => x.flatMap {
    case x: OntologyElement => Some(x.features)
    case _ => None
  })).foldLeft(mutable.Map().withDefaultValue(0.0): mutable.Map[String, Double])((acc, x) => OntologyElement.joinFeatures(acc, x))

  /**
    * Updates state of all ContextElements with OntologyElement in main element
    *
    * @param trainSet        train set with filtered context elements
    * @param categoryWeights Map of category weights
    */
  def updateStates(trainSet: List[List[ContextElement]], categoryWeights: scala.collection.Map[String, Double]): Unit = {
    trainSet.foreach(seq => {
      seq.foreach(elem => {
        var wikilinksFeatures: Option[Map[String, Double]] = None
        val localMap: mutable.Map[String, Double] = mutable.Map().withDefaultValue(0.0)
        elem.foldLeft(localMap) {
          case (acc, x: OntologyElement) =>
            if (x.weight == 1.0) wikilinksFeatures = Some(x.features)
            OntologyElement.joinFeatures(acc, x.features)
          case (acc, _) => acc
        }
        val (state, _) = wikilinksFeatures.getOrElse(localMap).maxBy { case (k, v) => v * categoryWeights.getOrElse(k, 0.0) }
        elem.label = state
      })
    })
  }

  type SkNNClassifier = SkNN[BaseElement, SkNNNode[BaseElement]]
  type SkNNModel = Model[BaseElement, SkNNNode[BaseElement]]

  def learnModel(trainingSet: List[List[ContextElement]]): (SkNNClassifier, SkNNModel) = {
    val model: SkNNModel = new SkNNModel((label) => {
      new SkNNNodeImpl[BaseElement, PlainAverageStorage[BaseElement]](label, 1)(() => {
        new PlainAverageStorage[BaseElement](ElementMeasures.bagOfWordElementDistance)
      })
    })

    trainingSet.foreach(model.processSequence)

    val sknn = new SkNNClassifier(model)

    (sknn, model)
  }

  /**
    * This function should filter acceptable nodes for vertex
    */
  val filterNodes: (BaseElement, SkNNNode[BaseElement]) => Boolean = (_: BaseElement, node: SkNNNode[BaseElement]) => true

  /* {
    elem match {
      case contextElement: ContextElement => contextElement.mainElement match {
        case bow: BagOfWordElement => bow.label == node.label
        case _ => true
      }
      case _ => true
    }
  } */

  def analyse(sentence: String): Unit = {

    /**
      * Prepare target sentence
      */
    val objects = prepareSentence(sentence)

    /**
      * Get context element description
      */
    val target: List[ContextElement] = convertToContext(objects)

    /**
      * All concepts with disambiguation
      */
    val conceptsToLearn: List[(String, String, String)] = SentenceAnalizer.getConceptsToLearn(objects, contextSize)

    /**
      * Prepare training set from disambiguation
      */

    val trainingSet: List[List[ContextElement]] = getTrainingSet(conceptsToLearn)

    /**
      * Update of the states
      */
    val categories = getAllWeightedCategories(trainingSet)
    updateStates(trainingSet, categories)


    // TODO: resolve concepts redirects
    // TODO: Filter valuable concepts

    /**
      * Learn model
      */
    val (sknn, model) = learnModel(trainingSet)

    /**
      * Tag sequence, return possible combinations with weight
      * < List[(List[Node], Double)] >
      */
    val res = sknn.tag(target, 1)(filterNodes)


    val recoveredResult1 = RecoverConcept.recover(target, model.initNode, res.head._1)
    println(s"Weight: ${res.head._2}")
    objects.zip(recoveredResult1).foreach({ case (obj, node) => println(s"${obj.tokens.mkString(" ")} => ${node.label}") })

  }

}
