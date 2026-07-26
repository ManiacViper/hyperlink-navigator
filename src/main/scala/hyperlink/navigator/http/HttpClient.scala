package hyperlink.navigator.http

import cats.effect.{IO, Resource}
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

import scala.concurrent.duration.DurationInt

object HttpClient {

  def apply(): Resource[IO, Client[IO]] =
    EmberClientBuilder
      .default[IO]
      .withTimeout(5.seconds)
      .withMaxTotal(10)
      .withMaxPerKey(_ => 2)
      .build

}
