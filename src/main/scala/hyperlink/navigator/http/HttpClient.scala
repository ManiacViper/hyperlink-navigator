package hyperlink.navigator.http

import cats.effect.{IO, Resource}
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

object HttpClient {

  def apply(): Resource[IO, Client[IO]] =
    EmberClientBuilder
    .default[IO]
    .build

}
