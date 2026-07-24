package hyperlink.navigator.service

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import com.comcast.ip4s.IpLiteralSyntax
import hyperlink.navigator.domain.HtmlPage
import hyperlink.navigator.service.UrlServiceSpec.withTestServerSetup
import org.http4s.HttpRoutes
import org.http4s.Response.http4sKleisliResponseSyntaxOptionT
import org.http4s.dsl.io._
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.net.URI
import scala.concurrent.duration.Duration

class UrlServiceSpec extends AnyWordSpec with Matchers {
  "UrlService" should {
    "fetch html page" when {
      "url is valid" in {
        withTestServerSetup("/test-api/html-page") { case Right(htmlPage) =>
          htmlPage.document.title mustBe "Test page"
        }
      }
    }

    "fail" when {
      "there is an invalid url" in {
        withTestServerSetup("/does-not-exist") { case Left(exception) =>
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
      .withIdleTimeInPool(Duration.Zero)
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

  private val testPageService =
    HttpRoutes
      .of[IO] { case GET -> Root / "test-api" / "html-page" =>
        Ok(testHtmlPage)
      }
      .orNotFound

  private val testServer: Resource[IO, Server] = EmberServerBuilder
    .default[IO]
    .withShutdownTimeout(Duration.Zero)
    .withHost(ipv4"127.0.0.1")
    .withPort(port"8080")
    .withHttpApp(testPageService)
    .build

  def withTestServerSetup(path: String)(fn: Either[Throwable, HtmlPage] => Unit): Unit = {
    (for {
      client <- testHttpClient
      server <- testServer
    } yield (client, server))
      .use { case (client, server) =>
        val uri = new URI(s"http://127.0.0.1:8080$path")
        UrlService(client)
          .fetch(uri)
          .attempt
          .map(fn)
      }
      .unsafeRunSync()
  }

}
