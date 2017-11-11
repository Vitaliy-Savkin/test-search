package search

import scala.concurrent.Future
import scala.util.{Failure, Success}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

class WorkerServerImpl {
  private var serverBinding: ServerBinding = null

  private implicit val system = ActorSystem("my-system")
  private implicit val materializer = ActorMaterializer()
  private implicit val executionContext = system.dispatcher

  def run(args: Array[String]): Future[Unit] = {
    val port = args(0).toInt

    val index = new InvertedIndex()

    val route =
      (post & path("documents" / Segment)) { id =>
        entity(as[String]) { document =>
          index.putDocument(id, document)
          complete("")
        }
      } ~
      (get & path("documents" / Segment)) { id =>
        complete(index.getDocument(id).getOrElse(throw new Exception("No document found")))
      } ~
      (post & path("search")) {
        entity(as[String]) { tokens =>
          val docs = index.getDocuments(Client.parseTokens(tokens))
          complete(Client.stringifyTokens(docs.toSeq))
        }
      }

    val runF = Http().bindAndHandle(route, "127.0.0.1", port.toInt)
    runF.onComplete {
      case Success(b) =>
        serverBinding = b
        println(s"Server started at http://127.0.0.1:$port/")
      case Failure(t) =>
        println("Unable to start server: " + t)
    }
    runF.map(_ => ())
  }

  def shutdown(): Future[Unit] = {
    for {
      _ <- serverBinding.unbind()
      _ <- system.terminate()
    } yield ()
  }
}

object WorkerServer extends WorkerServerImpl {
  def main(args: Array[String]): Unit = run(args)
}
