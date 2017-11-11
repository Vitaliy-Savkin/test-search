package search

import java.util.concurrent.TimeUnit

import scala.concurrent.{Await, Future}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}

class SearchSpec extends FlatSpec with BeforeAndAfterAll with Matchers with Eventually {
  def result[T](f: Future[T]): T =
    Await.result(f, scala.concurrent.duration.Duration(5, TimeUnit.SECONDS))

  val master = new MasterServerImpl()
  val firstWorker = new WorkerServerImpl()
  val secondWorker = new WorkerServerImpl()

  override protected def beforeAll(): Unit = {
    result(master.run(Array("10000", "127.0.0.1:10101", "127.0.0.1:10102")))
    result(firstWorker.run(Array("10101")))
    result(secondWorker.run(Array("10102")))
  }

  override protected def afterAll(): Unit = {
    result(master.shutdown())
    result(firstWorker.shutdown())
    result(secondWorker.shutdown())
  }

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()

  val masterClient = new Client("127.0.0.1:10000")
  val firstWorkerClient = new Client("127.0.0.1:10101")
  val secondWorkerClient = new Client("127.0.0.1:10102")

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(5, Seconds), Span(200, Millis))

  val documents = Map(
    "1" -> "1   2 6 7",
    "2" -> "1\n2\n 3 4 5",
    "3" -> "5",
    "4" -> "6",
    "5" -> ""
  )

  "master" should "save documents" in {
    documents.foreach { case (id, document) =>
      masterClient.putDocument(id, document)
    }
  }

  "master" should "return documents by id" in {
    documents.foreach { case (id, document) =>
      result(masterClient.getDocument(id)) shouldBe document
    }
  }

  "master" should "search documents" in {
    def check(tokens: List[String], ids: List[String]) =
      result(masterClient.searchDocuments(tokens)) should contain theSameElementsAs ids
    
    check(List("7"), List("1"))
    check(List("5"), List("2", "3"))
    check(List("1", "2"), List("1", "2"))
    check(List("5", "6"), List.empty)
    check(List("100"), List.empty)
    check(List.empty, List.empty)
  }

  "workers" should "share documents" in {
    documents.foreach { case (id, document) =>
      val client = if (id.hashCode % 2 == 0) firstWorkerClient else secondWorkerClient
      result(client.getDocument(id)) shouldBe document
    }
  }
}