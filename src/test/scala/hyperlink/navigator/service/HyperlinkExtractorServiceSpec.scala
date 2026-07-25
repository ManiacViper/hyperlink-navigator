package hyperlink.navigator.service

import hyperlink.navigator.domain.{ExtractedHyperlink, HtmlPage}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.net.URI

class HyperlinkExtractorServiceSpec extends AnyWordSpec with Matchers {
  val jsoupBrowser = JsoupBrowser()

  "HyperlinkExtractorService" should {
    "extract hyperlinks from anchors" when {
      "anchor tags exist" in {
        val document =
          """
            |<html>
            |  <body>
            |    <a href="https://example.com/page1">Page 1</a>
            |    <a href="/about">About</a>
            |  </body>
            |</html>
            |""".stripMargin
        val originalUri = URI.create("some-uri")
        val input       = HtmlPage(originalUri, jsoupBrowser.parseString(document))
        val result      = HyperlinkExtractorService().extract(input)
        result must contain theSameElementsAs List(
          ExtractedHyperlink(
            originalUri,
            List(URI.create("https://example.com/page1"), URI.create("/about"))
          )
        )
      }
    }
  }
}
