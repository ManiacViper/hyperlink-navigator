package hyperlink.navigator

import cats.data.Kleisli
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import com.comcast.ip4s.{IpLiteralSyntax, Port}

import org.http4s.dsl.io.{GET, Ok, Root}
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.Response._
import org.http4s.client.Client
import org.http4s.dsl.io._

import scala.concurrent.duration.DurationInt
import cats.syntax.traverse._
import cats.instances.list._

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
                   <a href="https://first-url.com">Page 1</a>
                   <a href="/another-url">About</a>
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

  val non200: Kleisli[IO, Request[IO], Response[IO]] =
    HttpRoutes
      .of[IO] { case GET -> Root / "test-api" / "moved" =>
        MovedPermanently()
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

  case class TestServiceAndPort(service: Kleisli[IO, Request[IO], Response[IO]], port: Int)

  def withTestServerSetup(testServiceAndPort: List[TestServiceAndPort])(
    fn: Client[IO] => Unit
  ): Unit = {
    val servers =
      testServiceAndPort.map { case TestServiceAndPort(service, port) =>
        testServer(service, port)
      }.sequence

    (for {
      client <- testHttpClient
      _      <- servers
    } yield client)
      .use { client =>
        IO(fn(client))
      }
      .unsafeRunSync()
  }
}
