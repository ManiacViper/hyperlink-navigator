package hyperlink.navigator.service

import hyperlink.navigator.domain.{ExtractedHyperlinks, HtmlPage, RawHtmlPage}
import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._

import java.net.URI
import scala.util.Try
import cats.syntax.either._

trait HyperlinkExtractorService {
  def parse(rawHtmlPage: RawHtmlPage): Either[String, HtmlPage]
  def extract(htmlPage: HtmlPage): ExtractedHyperlinks
}

object HyperlinkExtractorService {
  private val jsoupBrowser: Browser = JsoupBrowser()
  def apply(): HyperlinkExtractorService = new HyperlinkExtractorService {
    override def extract(htmlPage: HtmlPage): ExtractedHyperlinks = {
      val maybeHyperlinks: Option[List[URI]] = {
        val maybeHrefs = htmlPage.document >?>
          elementList("a") >?>
          attr("href").map(URI.create)

        maybeHrefs
          .map { maybeHrefs =>
            maybeHrefs.collect { case Some(uri) =>
              uri
            }
          }
      }

      ExtractedHyperlinks(htmlPage.uri, maybeHyperlinks.getOrElse(List.empty))
    }

    override def parse(rawHtmlPage: RawHtmlPage): Either[String, HtmlPage] = {
      Try(jsoupBrowser.parseString(rawHtmlPage.rawDocument)).toEither
        .leftMap { ex =>
          s"Page for [Url=${rawHtmlPage.uri}] has an error, ${ex.getMessage}"
        }
        .map(document => HtmlPage(rawHtmlPage.uri, document))
    }
  }
}
