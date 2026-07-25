package hyperlink.navigator

import cats.data.Kleisli
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import com.comcast.ip4s.{IpLiteralSyntax, Port}
import hyperlink.navigator.domain.HtmlPage
import hyperlink.navigator.service.UrlService
import org.http4s.dsl.io.{GET, Ok, Root}
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.Response._
import org.http4s.client.Client
import org.http4s.dsl.io._

import java.net.URI
import scala.concurrent.duration.DurationInt

object TestUrlServer {
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
  val emptyHtmlPage = """<html>
                        | <head></head>
                        | <body></body>
                        |</html>""".stripMargin

  val testPageService: Kleisli[IO, Request[IO], Response[IO]] =
    HttpRoutes
      .of[IO] { case GET -> Root / "test-api" / "html-page" =>
        Ok(testHtmlPage)
      }
      .orNotFound

  val noHtmlPageService: Kleisli[IO, Request[IO], Response[IO]] =
    HttpRoutes
      .of[IO] { case GET -> Root / "test-api" / "empty-html-page" =>
        Ok()
      }
      .orNotFound
  private val testHttpClient =
    EmberClientBuilder
      .default[IO]
      .withIdleTimeInPool(1.second)
      .build
  private def testServer(
                          service: Kleisli[IO, Request[IO], Response[IO]],
                          port: Int
                        ): Resource[IO, Server] = {
    val portObj = Port.fromInt(port)
    EmberServerBuilder
      .default[IO]
      .withShutdownTimeout(1.seconds)
      .withHost(ipv4"127.0.0.1")
      .withPort(portObj.get)
      .withHttpApp(service)
      .build
  }

  def withTestServerSetup(
                           service: Kleisli[IO, Request[IO], Response[IO]],
                           port: Int = 8081
                         )(
                           fn: Client[IO] => Unit
                         ): Unit = {
    (for {
      client <- testHttpClient
      _      <- testServer(service, port)
    } yield client)
      .use { client =>
        IO(fn(client))
      }
      .unsafeRunSync()
  }
}
