package hyperlink.navigator

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.io.file.{Files, Path}
import hyperlink.navigator.StreamingAppSpec.TestExtractedUrlResult
import hyperlink.navigator.repository.FileReaderRepository
import hyperlink.navigator.service.InputValidatorService
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class StreamingAppSpec extends AnyWordSpec with Matchers {
  private val resultsFile = "src/test/resources/test-extracted-urls.csv"

  "StreamingApp" should {
    "read input urls and write extracted urls" when {
      "there are a list of input urls to process" in {
        StreamingApp
          .stream(
            "test-urls.csv",
            resultsFile,
            FileReaderRepository(),
            InputValidatorService()
          )
          .unsafeRunSync()

        val results = StreamingAppSpec.readResults(resultsFile).compile.toList.unsafeRunSync
        val expected =
          List(
            TestExtractedUrlResult("https://www.google.com/search?q=blog+websites"),
            TestExtractedUrlResult("https://www.bing.com/search"),
            TestExtractedUrlResult("https://www.duckduckgo.com/")
          )
        results mustBe expected
      }
    }
  }

}

object StreamingAppSpec {
  private case class TestExtractedUrlResult(url: String)
  private def readResults(path: String) = {
    Files[IO]
      .readUtf8Lines(Path(path))
      .map { line: String =>
        TestExtractedUrlResult(line)
      }
      .evalTap { _ =>
        Files[IO].deleteIfExists(Path(path))
      }
  }
}
