package search

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

import scala.collection.JavaConverters._

class ConcurrentMultiMap[K, V] {
  private val values = new ConcurrentHashMap[K, ConcurrentLinkedQueue[V]]

  def add(key: K, value: V): Unit = {
    var queue = values.get(key)
    if (queue == null) {
      val newQueue = new ConcurrentLinkedQueue[V]
      val oldQueue = values.putIfAbsent(key, newQueue)
      if (oldQueue == null) queue = newQueue
      else queue = oldQueue
    }
    queue.add(value)
  }

  def values(key: K): Seq[V] =
    Option(values.get(key)).map(_.asScala.toSeq).getOrElse(Seq.empty)
}