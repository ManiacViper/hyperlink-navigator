package hyperlink.navigator

import cats.effect.{IO, IOApp}
import hyperlink.navigator.StreamingApp.build

object Main extends IOApp.Simple {
  def run: IO[Unit] =
    build("urls.csv", "extracted-urls.csv")
}
