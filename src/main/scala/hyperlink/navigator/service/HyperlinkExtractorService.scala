package hyperlink.navigator.service

import hyperlink.navigator.domain.{ExtractedHyperlink, HtmlPage}
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import java.net.URI

trait HyperlinkExtractorService {
  def extract(htmlPage: HtmlPage): List[ExtractedHyperlink]
}

object HyperlinkExtractorService {
  def apply(): HyperlinkExtractorService = new HyperlinkExtractorService {
    override def extract(htmlPage: HtmlPage): List[ExtractedHyperlink] = {
      val maybeRawHyperlinks =
        htmlPage.document >?>
          elementList("a") >>
          attr("href")
            .map(URI.create)

      List(ExtractedHyperlink(htmlPage.uri, maybeRawHyperlinks.getOrElse(List.empty)))
    }
  }
}
