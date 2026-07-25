package hyperlink.navigator

package ice.finance

import cats.effect.IOApp
import hyperlink.navigator.StreamingApp.runStream
import hyperlink.navigator.http.HttpClient
import hyperlink.navigator.repository.FileReaderRepository
import hyperlink.navigator.service.{InputValidatorService, UrlService}

object Main extends IOApp.Simple {
  def run =
    runStream
}
