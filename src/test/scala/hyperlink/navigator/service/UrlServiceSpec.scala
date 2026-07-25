package hyperlink.navigator.service

import cats.data.Kleisli
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import com.comcast.ip4s.IpLiteralSyntax
import hyperlink.navigator.domain.HtmlPage
import hyperlink.navigator.service.UrlServiceSpec.{
  emptyHtmlPage,
  noHtmlPageService,
  testPageService,
  withTestServerSetup
}
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
        withTestServerSetup("/test-api/html-page", testPageService) { case Right(result) =>
          result.uri mustBe new URI("http://127.0.0.1:8080/test-api/html-page")
          result.document.title mustBe "Test page"
        }
      }

      "there is no response body" in {
        withTestServerSetup("/test-api/empty-html-page", noHtmlPageService) { case Right(result) =>
          result.uri mustBe new URI("http://127.0.0.1:8080/test-api/empty-html-page")
          result.document.toHtml mustBe JsoupBrowser().parseString(emptyHtmlPage).toHtml
        }
      }
    }

    "fail" when {
      "there is an invalid url" in {
        withTestServerSetup("/does-not-exist", testPageService) { case Left(exception) =>
          exception.getMessage mustBe "unexpected HTTP status: 404 Not Found for request GET http://127.0.0.1:8080/does-not-exist"
        }
      }
    }
  }

}

object UrlServiceSpec {
  private val testHttpClient =
    EmberClientBuilder
      .default[IO]
      .withIdleTimeInPool(1.second)
      .build

  private val testHtmlPage = """
      <!DOCTYPE html>
      <html lang="en">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title>Test page</title>
        </head>
            <body>
              <div id="header">
                <h1>Some Test page h1</h1>
              </div>
            </body>
      </html>
      """
  private val emptyHtmlPage = """<html>
                                | <head></head>
                                | <body></body>
                                |</html>""".stripMargin

  private val testPageService: Kleisli[IO, Request[IO], Response[IO]] =
    HttpRoutes
      .of[IO] { case GET -> Root / "test-api" / "html-page" =>
        Ok(testHtmlPage)
      }
      .orNotFound

  private val noHtmlPageService: Kleisli[IO, Request[IO], Response[IO]] =
    HttpRoutes
      .of[IO] { case GET -> Root / "test-api" / "empty-html-page" =>
        Ok()
      }
      .orNotFound

  private def testServer(service: Kleisli[IO, Request[IO], Response[IO]]): Resource[IO, Server] =
    EmberServerBuilder
      .default[IO]
      .withShutdownTimeout(1.seconds)
      .withHost(ipv4"127.0.0.1")
      .withPort(port"8080")
      .withHttpApp(service)
      .build

  def withTestServerSetup(path: String, service: Kleisli[IO, Request[IO], Response[IO]])(
    fn: Either[Throwable, HtmlPage] => Unit
  ): Unit = {
    (for {
      client <- testHttpClient
      _      <- testServer(service)
    } yield client)
      .use { case client =>
        val uri = new URI(s"http://127.0.0.1:8080$path")
        UrlService(client)
          .fetch(uri)
          .attempt
          .map(fn)
      }
      .unsafeRunSync()
  }

}
