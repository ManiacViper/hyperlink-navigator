package hyperlink.navigator.service

import cats.effect.IO
import hyperlink.navigator.domain.RawHtmlPage
import net.ruippeixotog.scalascraper.browser.JsoupBrowser

import java.net.URI
import org.http4s.client.Client

trait UrlService {
  def fetch(uri: URI): IO[RawHtmlPage]
}

object UrlService {
  def apply(httpClient: Client[IO]): UrlService = new UrlService {
    override def fetch(uri: URI): IO[RawHtmlPage] = {
      httpClient
        .expect[String](uri.toString)
        .map { rawDocStr =>
          RawHtmlPage(uri, rawDocStr)
        }

    }
  }
}
