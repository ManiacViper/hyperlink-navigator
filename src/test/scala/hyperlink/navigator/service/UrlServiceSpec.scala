package hyperlink.navigator.service

import cats.effect.unsafe.implicits.global
import hyperlink.navigator.TestUrlServer._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.net.URI

class UrlServiceSpec extends AnyWordSpec with Matchers {
  "UrlService" should {
    "fetch html page" when {
      "url is valid" in {
        withTestServerSetup(List(TestServiceAndPort(testPageService, 8081))) { client =>
          val Right(result) = UrlService(client)
            .fetch(URI.create("http://127.0.0.1:8081/test-api/html-page"))
            .attempt
            .unsafeRunSync()

          result.uri mustBe new URI("http://127.0.0.1:8081/test-api/html-page")
          result.rawDocument mustBe testHtmlPage
        }
      }

      "there is no response body" in {
        withTestServerSetup(List(TestServiceAndPort(noHtmlPageService, 8081))) { client =>
          val Right(result) = UrlService(client)
            .fetch(URI.create("http://127.0.0.1:8081/test-api/empty-html-page"))
            .attempt
            .unsafeRunSync()

          result.uri mustBe new URI("http://127.0.0.1:8081/test-api/empty-html-page")
          result.rawDocument mustBe ""
        }
      }
    }

    "fail" when {
      "there is a non-200 code" in {
        withTestServerSetup(List(TestServiceAndPort(testPageService, 8081))) { client =>
          val Left(exception) = UrlService(client)
            .fetch(URI.create("http://127.0.0.1:8081/does-not-exist"))
            .attempt
            .unsafeRunSync()

          exception.getMessage mustBe "unexpected HTTP status: 404 Not Found for request GET http://127.0.0.1:8081/does-not-exist"
        }
      }
    }
  }

}
