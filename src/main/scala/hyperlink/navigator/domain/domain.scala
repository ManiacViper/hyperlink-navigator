package hyperlink.navigator.domain

import java.net.URI
import scala.util.Try
import cats.syntax.either._

case class ValidatedUrl(uri: URI)
object ValidatedUrl {
  def from(value: String): Either[String, ValidatedUrl] = {
    Try(URI.create(value))
      .toEither
      .leftMap(ex => ex.getMessage)
      .flatMap { uri =>
        Either.cond[String, ValidatedUrl](uri.isAbsolute && uri.getHost.nonEmpty, ValidatedUrl(uri), s"invalid url, [${uri.toString}]")
      }
  }
}