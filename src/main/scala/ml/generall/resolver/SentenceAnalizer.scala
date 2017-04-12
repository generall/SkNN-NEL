package ml.generall.resolver

import com.typesafe.scalalogging.Logger
import ml.generall.ner.elements.{ContextElement, _}
import ml.generall.ner.{ElementMeasures, Measures, RecoverConcept}
import ml.generall.nlp.ChunkRecord
import ml.generall.resolver.dto.{ConceptDescription, ConceptVariant, ConceptsAnnotation}
import ml.generall.resolver.tools.{Hyperparams, Tools}
import ml.generall.sknn.SkNN
import ml.generall.sknn.model.storage.PlainAverageStorage
import ml.generall.sknn.model.storage.elements.BaseElement
import ml.generall.sknn.model.{Model, SkNNNode, SkNNNodeImpl}

import scala.collection.mutable


object SentenceAnalizer {

  val elementCache: mutable.HashMap[String, OntologyElement] = new mutable.HashMap[String, OntologyElement]()

  def clearCache = elementCache.clear

  var hits = 0
  var total = 0

  def getOntologyElement(concept: String, weight: Double): OntologyElement = {
    val dbpediaConcept = SentenceAnalizer.wikiToDbpedia(concept)
    total += 1
    if (elementCache.contains(dbpediaConcept)) {
      hits += 1
    }
    elementCache.getOrElseUpdate(dbpediaConcept, new OntologyElement(dbpediaConcept, conceptWeight = weight))
  }

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
      case List(concept) =>
        val multi = new MultiElement[WeightedSetElement]
        val onto = getOntologyElement(concept.concept, concept.getWeight)
        if (onto.features.nonEmpty)
          multi.addElement(onto)
        multi.addElement(element)
        multi.label = obj.state // multi.genLabel
        multi
      case disambiguation: Iterable[ConceptVariant] =>
        val multi = new MultiElement[WeightedSetElement]

        disambiguation
          .view
          .map(x => getOntologyElement(x.concept, x.getWeight))
          .filter(element => element.features.nonEmpty)
          .foreach(multi.addElement)

        multi.addElement(element)
        multi.label = obj.state
        multi
    }
  }

}

/**
  * Created by generall on 27.08.16.
  */
class SentenceAnalizer {

  val logger = Logger("Analyzer")

  val contextSize = Hyperparams.CONTEXT_SIZE
  val searcher = Searcher
  val parser = LocalCoreNLP
  val exampleBuilder = new ExamplesBuilder

  val NERRemapping = Map(
    "NUMBER" -> "O",
    "DATE" -> "O",
    "TIME" -> "O"
  )

  def prepareSentence(sentence: String): (List[TrainObject], List[(Int, Int)]) = {
    val parseRes = Tools.time(parser.process(sentence), "parser")

    val groups = parseRes.zipWithIndex
      .groupBy({ case (record, _) => (record.parseTag, /* NERRemapping.getOrElse(record.ner, record.ner), */ record.groupId) })
      .toList
      .sortBy(x => x._2.head._2 /* first id of chunk */)
      .map {
        case ((tag, _), chunksWithIdx) =>
          val chunks = chunksWithIdx.map(_._1)
          val hasNer = chunks.exists( chunk => NERRemapping.getOrElse(chunk.ner, chunk.ner) != "O" )
          val filteredChunks = if (hasNer)
            chunks.filter(x => x.ner != "O")
          else
            chunks
          (tag, filteredChunks)
      }

      //.map(pair => (s"${pair._1._1}" /* _${pair._1._2} */ , pair._2.map(_._1))) // creation of state

    val annotations: List[(Int, Int)] = groups.map { case (_, chunks) => {
      val from = chunks.head.beginPos
      val to = chunks.last.endPos
      (from, to)
    }
    }

    (exampleBuilder.makeTrain(groups), annotations)
  }


  /**
    * Filter predicate, keep only OntologyElements
    *
    * @return keep element?
    */
  def filterSequencePredicate(el: (ContextElement, Int)): Boolean = el._1.mainElement match {
    case x: MultiElement[_] => x.subElements.exists {
      case y: OntologyElement => y.nonEmpty
      case _ => false
    }
    case y: OntologyElement => y.nonEmpty
    case _ => false
  }

  /**
    * Prepare training set for disambiguation
    */
  def getTrainingSet(conceptsToLearn: List[(String, String, String)]): List[List[ContextElement]] = conceptsToLearn
    .par
    .flatMap(x => exampleBuilder.build(x._2, x._1, x._3))
    .filter(_.nonEmpty)
    .map(convertToContext)
    .map(_.unzip._1)
    .toList

  def filterSequence(seq: List[ContextElement]): List[(ContextElement, Int)] = seq.zipWithIndex.filter(filterSequencePredicate)


  def convertToContext(objects: List[TrainObject]): List[(ContextElement, Int)] = filterSequence(
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
        elem.label = getElementState(elem, categoryWeights)
      })
    })
  }

  def getElementState(element: ContextElement, categoryWeights: scala.collection.Map[String, Double]): String = {
    var wikilinksFeatures: Option[Map[String, Double]] = None
    val localMap: mutable.Map[String, Double] = mutable.Map().withDefaultValue(0.0)
    element.foldLeft(localMap) {
      case (acc, x: OntologyElement) =>
        if (x.weight == 1.0) wikilinksFeatures = Some(x.features)
        OntologyElement.joinFeatures(acc, x.features)
      case (acc, _) => acc
    }
    val (state, _) = wikilinksFeatures.getOrElse(localMap).maxBy { case (k, v) => v * categoryWeights.getOrElse(k, 0.0) }

    state
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
  val filterNodes: (BaseElement, SkNNNode[BaseElement]) => Boolean = (elem: BaseElement, node: SkNNNode[BaseElement]) => {
    elem match {
      case contextElement: ContextElement => contextElement.mainElement match {
        case x: MultiElement[_] => x.exists {
          case y: WeightedSetElement => y.features.contains(node.label)
        }
        case _ => true
      }
      case _ => true
    }
  }

  def analyse(sentence: String): List[ConceptsAnnotation] = {

    // exampleBuilder.builder = BuilderMockup // TODO: remove this! For test only

    /**
      * Clear OntologyElement cache
      */
    SentenceAnalizer.clearCache

    /**
      * Prepare target sentence
      */
    val (objects: List[TrainObject], annotations) = prepareSentence(sentence)

    /**
      * Get context element description
      */
    val (target: List[ContextElement], selectedIds: List[Int]) = convertToContext(objects).unzip

    if (target.isEmpty)
      return Nil
    /**
      * All concepts with disambiguation
      */
    val conceptsToLearn: List[(String, String, String)] = SentenceAnalizer.getConceptsToLearn(objects, contextSize)

    /**
      * Prepare training set from disambiguation
      */

    val trainingSet: List[List[ContextElement]] = Tools.time(getTrainingSet(conceptsToLearn), "getTrainingSet")

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
    val res = Tools.time(sknn.tag(target, 1)(filterNodes), "tag")

    val relevantChunks: List[(Int, Int)] = selectedIds.map(id => annotations(id))
    val relevantObjects = selectedIds.map(id => objects(id))

    val recoveredResult1 = RecoverConcept.recover(target, model.initNode, res.head._1)

    val resAnnotations = relevantChunks.zip(recoveredResult1).map { case ((from, to), node) => ConceptsAnnotation(
      fromPos = from,
      toPos = to,
      concepts = List(ConceptDescription(
        concept = node.label,
        params = Map()
      ))
    )
    }

    logger.info("Dist calls: " + Measures.count)
    logger.info("Dist foo calls: " + ElementMeasures.count)
    logger.info("Model nodes: " + model.nodes.size)
    logger.info("Model elements: " + model.nodes.foldLeft(0)((acc, node) => acc + node._2.asInstanceOf[SkNNNodeImpl[_, _]].storages.size))
    logger.info("OntologyElement count: " + SentenceAnalizer.total)
    logger.info("OntologyElement cache hits: " + SentenceAnalizer.hits)

    resAnnotations
  }

}
