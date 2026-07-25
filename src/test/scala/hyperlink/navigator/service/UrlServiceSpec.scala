package hyperlink.navigator.service

import cats.data.Kleisli
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import com.comcast.ip4s.IpLiteralSyntax
import hyperlink.navigator.TestUrlServer.{emptyHtmlPage, noHtmlPageService, testPageService, withTestServerSetup}
import hyperlink.navigator.domain.HtmlPage
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import org.http4s.Response.http4sKleisliResponseSyntaxOptionT
import org.http4s.dsl.io._
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.{HttpRoutes, Request, Response}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.net.URI
import scala.concurrent.duration.DurationInt

class UrlServiceSpec extends AnyWordSpec with Matchers {
  "UrlService" should {
    "fetch html page" when {
      "url is valid" in {
        withTestServerSetup(testPageService) { client =>
          val Right(result) = UrlService(client).fetch(URI.create("http://127.0.0.1:8081/test-api/html-page")).attempt.unsafeRunSync()
          result.uri mustBe new URI("http://127.0.0.1:8081/test-api/html-page")
          result.document.title mustBe "Test page"
        }
      }

      "there is no response body" in {
        withTestServerSetup(noHtmlPageService) { client =>
          val Right(result) = UrlService(client).fetch(URI.create("http://127.0.0.1:8081/test-api/empty-html-page")).attempt.unsafeRunSync()

          result.uri mustBe new URI("http://127.0.0.1:8081/test-api/empty-html-page")
          result.document.toHtml mustBe JsoupBrowser().parseString(emptyHtmlPage).toHtml
        }
      }
    }

    "fail" when {
      "there is a non-200 code" in {
        withTestServerSetup(testPageService) { client =>
          val Left(exception) = UrlService(client).fetch(URI.create("http://127.0.0.1:8081/does-not-exist")).attempt.unsafeRunSync()
          exception.getMessage mustBe "unexpected HTTP status: 404 Not Found for request GET http://127.0.0.1:8081/does-not-exist"
        }
      }
    }
  }

}

