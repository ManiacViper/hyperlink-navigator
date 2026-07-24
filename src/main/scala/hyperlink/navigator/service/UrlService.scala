package hyperlink.navigator.service

import cats.effect.IO
import hyperlink.navigator.domain.HtmlPage
import net.ruippeixotog.scalascraper.browser.JsoupBrowser

import java.net.URI
import org.http4s.client.Client

trait UrlService {
  def fetch(uri: URI): IO[HtmlPage]
}

object UrlService {
  def apply(httpClient: Client[IO]): UrlService = new UrlService {
    override def fetch(uri: URI): IO[HtmlPage] = {
      httpClient
        .expect[String](uri.toString)
        .map { rawDocStr =>
          val jsoupBrowser = JsoupBrowser()
          val doc          = jsoupBrowser.parseString(rawDocStr)
          HtmlPage(uri, doc)
        }

    }
  }
}
