package hyperlink.navigator

package ice.finance

import cats.effect.IOApp
import hyperlink.navigator.repository.FileReaderRepository

object Main extends IOApp.Simple {
  def run = StreamingApp.stream("urls.csv", "extracted-urls.csv", FileReaderRepository())
}
