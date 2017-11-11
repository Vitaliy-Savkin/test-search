package search

import java.util.concurrent.ConcurrentHashMap

class InvertedIndex {
  private val invertedIndex = new ConcurrentMultiMap[String, String]
  private val documentsIndex = new ConcurrentHashMap[String, String]

  def putDocument(id: String, document: String): Unit = {
    documentsIndex.put(id, document)
    document.split("\\s+").foreach { token => invertedIndex.add(token, id) }
  }

  def getDocument(id: String): Option[String] = Option(documentsIndex.get(id))

  def getDocuments(tokens: Seq[String]): Set[String] = {
    tokens.map(token => invertedIndex.values(token).toSet).reduceOption(_ intersect _).getOrElse(Set.empty)
  }
}