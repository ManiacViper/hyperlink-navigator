package hyperlink.navigator.service

import hyperlink.navigator.domain.{ExtractedHyperlinks, HtmlPage, RawHtmlPage}
import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.net.URI

class HyperlinkExtractorServiceSpec extends AnyWordSpec with Matchers {
  val jsoupBrowser: Browser = JsoupBrowser()

  "HyperlinkExtractorService.extract" should {
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

        val expected = ExtractedHyperlinks(
          originalUri,
          List(URI.create("https://example.com/page1"), URI.create("/about"))
        )
        result.originalUri mustBe expected.originalUri
        result.extractedHyperLinks must contain theSameElementsAs expected.extractedHyperLinks
      }

      "there are some anchor tags without href attribute" in {
        val document =
          """
            |<html>
            |  <body>
            |    <a href="https://example.com/page1">Page 1</a>
            |    <a>About</a>
            |  </body>
            |</html>
            |""".stripMargin
        val originalUri = URI.create("some-uri")
        val input       = HtmlPage(originalUri, jsoupBrowser.parseString(document))
        val result      = HyperlinkExtractorService().extract(input)

        val expected = ExtractedHyperlinks(
          originalUri,
          List(URI.create("https://example.com/page1"))
        )
        result.originalUri mustBe expected.originalUri
        result.extractedHyperLinks must contain theSameElementsAs expected.extractedHyperLinks
      }
    }

    "return empty hyperlinks with original url" when {
      "no anchor tags exist" in {
        val document =
          """
            |<html>
            |  <body>
            |  </body>
            |</html>
            |""".stripMargin
        val originalUri = URI.create("some-uri")
        val input       = HtmlPage(originalUri, jsoupBrowser.parseString(document))
        val result      = HyperlinkExtractorService().extract(input)

        val expected = ExtractedHyperlinks(
          originalUri,
          List(URI.create("https://example.com/page1"))
        )
        result.originalUri mustBe expected.originalUri
        result.extractedHyperLinks must contain theSameElementsAs List.empty
      }
    }

    "HyperlinkExtractorService.parse" should {
      "parse a raw html value" when {
        "there is an html page" in {
          val rawDocument =
            """
              |<html>
              |  <body>
              |   <title>hello world</h1>
              |  </body>
              |</html>
              |""".stripMargin
          val originalUri   = URI.create("some-uri")
          val input         = RawHtmlPage(originalUri, rawDocument)
          val Right(result) = HyperlinkExtractorService().parse(input)

          result.uri mustBe originalUri
          result.document.toHtml mustBe jsoupBrowser.parseString(rawDocument).toHtml
        }

        "there is a string" in {
          val rawDocument   = "some-string"
          val originalUri   = URI.create("some-uri")
          val input         = RawHtmlPage(originalUri, rawDocument)
          val Right(result) = HyperlinkExtractorService().parse(input)

          result.uri mustBe originalUri
          result.document.toHtml mustBe jsoupBrowser.parseString(rawDocument).toHtml
        }

        "there is empty string" in {
          val rawDocument   = ""
          val originalUri   = URI.create("some-uri")
          val input         = RawHtmlPage(originalUri, rawDocument)
          val Right(result) = HyperlinkExtractorService().parse(input)

          result.uri mustBe originalUri
          result.document.toHtml mustBe jsoupBrowser.parseString(rawDocument).toHtml
        }
      }

      "fail" when {
        "document is null" in {
          val rawDocument: Null    = null
          val originalUri          = URI.create("some-uri")
          val input                = RawHtmlPage(originalUri, rawDocument)
          val Left(result: String) = HyperlinkExtractorService().parse(input)

          result mustBe "Page for [Url=some-uri] has an error, Cannot invoke \"String.length()\" because \"s\" is null"
        }
      }

    }
  }
}
