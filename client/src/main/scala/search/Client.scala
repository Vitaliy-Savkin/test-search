package search

import java.util.concurrent.TimeUnit

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.Materializer
import play.api.libs.json.{JsArray, Json, JsString}

class Client(url: String)(implicit m: Materializer, as: ActorSystem) {

  implicit val ec: ExecutionContext = as.dispatcher

  private def requestString(request: HttpRequest) =
    for {
      response <- Http().singleRequest(request)
      entity <- response.entity.toStrict(Duration(5, TimeUnit.SECONDS))
    } yield entity.data.decodeString("UTF-8")

  def putDocument(id: String, document: String): Future[Unit] = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = s"http://$url/documents/$id",
      entity = HttpEntity(document)
    )

    Http().singleRequest(request).map(_ => ())
  }

  def getDocument(id: String): Future[String] = {
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = s"http://$url/documents/$id"
    )
    requestString(request)
  }

  def searchDocuments(tokens: Seq[String]): Future[Seq[String]] = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = s"http://$url/search",
      entity = HttpEntity(Client.stringifyTokens(tokens))
    )
    requestString(request).map(Client.parseTokens)
  }
}

object Client {
  def stringifyTokens(tokens: Seq[String]): String =
    Json.stringify(JsArray(tokens.map(JsString)))

  def parseTokens(tokens: String): Seq[String] =
    Json.parse(tokens)
      .asInstanceOf[JsArray].value
      .map(_.asInstanceOf[JsString].value)
}