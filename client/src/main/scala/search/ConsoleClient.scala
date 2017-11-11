package search

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object ConsoleClient {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def printUsage() = {
    println("Usage:")
    println("master-url -put id document")
    println("master-url -get id")
    println("master-url -search id token1 token2 ...")
  }

  def executeAction[T](f: Future[T])(onComplete: Try[T] => Unit) = {
    f.onComplete(onComplete)
    for {
      _ <- f
      _ <- system.terminate()
    } yield materializer.shutdown()
  }

  def main(args: Array[String]): Unit = {
    args match {
      case Array("-help") =>
        executeAction(Future.successful(printUsage()))(Function.const(()))

      case Array(url, "-put", id, document) =>
        executeAction(new Client(url).putDocument(id, document)) {
          {
            case Success(_) => println("Documents added")
            case Failure(t) => println(t.getMessage)
          }
        }
      case Array(url, "-get", id) =>
        executeAction(new Client(url).getDocument(id)) {
          {
            case Success(document) => println(document)
            case Failure(t) => println(t.getMessage)
          }
        }

      case Array(url, "-search", tokens @ _*) =>
        executeAction(new Client(url).searchDocuments(tokens)) {
          case Success(documents) =>
            documents.foreach { doc =>
              println("-" * 20)
              println(doc)
            }
          case Failure(t) =>
            println(t.getMessage)
        }

      case _ =>
        println("Wrong arguments")
        executeAction(Future.successful(printUsage()))(Function.const(()))
    }
  }
}
