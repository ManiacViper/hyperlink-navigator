package hyperlink.navigator.service

import hyperlink.navigator.domain.{ExtractedHyperlinks, HtmlPage}
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import java.net.URI

trait HyperlinkExtractorService {
  def extract(htmlPage: HtmlPage): ExtractedHyperlinks
}

object HyperlinkExtractorService {
  def apply(): HyperlinkExtractorService = new HyperlinkExtractorService {
    override def extract(htmlPage: HtmlPage): ExtractedHyperlinks = {
      val maybeRawHyperlinks =
        htmlPage.document >?>
          elementList("a") >>
          attr("href")
            .map(URI.create)

      ExtractedHyperlinks(htmlPage.uri, maybeRawHyperlinks.getOrElse(List.empty))
    }
  }
}
