package search

import scala.concurrent.Future
import scala.util.{Failure, Success}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

class MasterServerImpl {
  private var serverBinding: ServerBinding = null

  private implicit val system = ActorSystem("my-system")
  private implicit val materializer = ActorMaterializer()
  private implicit val executionContext = system.dispatcher

  def run(args: Array[String]): Future[Unit] = {
    val Array(port, workersUrls @ _*) = args

    val workersClients = workersUrls.map(url => new Client(url))

    def workerById(id: String) = workersClients(id.hashCode % workersClients.size)

    val route =
      (post & path("documents" / Segment)) { id =>
        entity(as[String]) { document =>
          complete(workerById(id).putDocument(id, document).map(_ => ""))
        }
      } ~
      (get & path("documents" / Segment)) { id =>
        complete(workerById(id).getDocument(id))
      } ~
      (post & path("search")) {
        entity(as[String]) { tokens =>
          val fs = workersClients.map { client =>
            client.searchDocuments(Client.parseTokens(tokens))
          }
          complete(Future.sequence(fs).map(_.flatten).map(Client.stringifyTokens))
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

object MasterServer extends MasterServerImpl {
  def main(args: Array[String]): Unit = run(args)
}

