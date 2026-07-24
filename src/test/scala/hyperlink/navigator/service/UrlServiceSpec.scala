package hyperlink.navigator.service

import cats.data.Kleisli
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import com.comcast.ip4s.IpLiteralSyntax
import hyperlink.navigator.domain.HtmlPage
import hyperlink.navigator.http.HttpClient
import hyperlink.navigator.service.UrlServiceSpec.{testHtmlPage, withTestServerSetup}
import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.Response.http4sKleisliResponseSyntaxOptionT
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.net.URI

class UrlServiceSpec extends AnyWordSpec with Matchers {
  "UrlService" should {
    "fetch html page" when {
      "url is valid" in {
        withTestServerSetup("http://localhost:8080/test-api/html-page") { htmlPage =>
          htmlPage.document.title mustBe "Test page"
        }
      }
    }
  }

}

object UrlServiceSpec {
  val testHttpClient =
    EmberClientBuilder
      .default[IO]
      .build

  val testHtmlPage = """
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

  val testServer: Resource[IO, Server] = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"127.0.0.1")
    .withPort(port"8080")
    .withHttpApp(testPageService)
    .build

  def withTestServerSetup(uriString: String)(fn: HtmlPage => Unit): Unit = {
    (for {
      client <- testHttpClient
      _      <- testServer
    } yield client)
      .use { client =>
        IO {
          val uri        = new URI(uriString)
          val htmlResult = UrlService(client).fetch(uri)
          htmlResult.map(fn(_))
        }
          .unsafeRunSync()
      }
  }

}
