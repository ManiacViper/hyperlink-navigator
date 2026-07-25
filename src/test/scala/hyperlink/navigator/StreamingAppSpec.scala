package hyperlink.navigator

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.io.file.{Files, Path}
import hyperlink.navigator.StreamingAppSpec.TestExtractedUrlResult
import hyperlink.navigator.TestUrlServer.{TestServiceAndPort, noHtmlPageService, non200, testPageService, withTestServerSetup}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class StreamingAppSpec extends AnyWordSpec with Matchers {
  private val resultsFile = "src/test/resources/test-extracted-urls.csv"

  "StreamingApp" should {
    "read input urls and write extracted urls" when {
      "an input url exists" in {
        withTestServerSetup(List(TestServiceAndPort(testPageService, 8080))) { _ =>
          StreamingApp
            .build("e2e-url.csv", resultsFile)
            .unsafeRunSync()

          val results = StreamingAppSpec.readResults(resultsFile).compile.toList.unsafeRunSync
          val expected =
            List(
              TestExtractedUrlResult("http://127.0.0.1:8080/test-api/html-page")
            )
          results mustBe expected
        }
      }
    }

    "skip urls" when {
      "an input url fails" in {
        withTestServerSetup(List(TestServiceAndPort(testPageService, 8090), TestServiceAndPort(non200, 8080))) { _ =>
          StreamingApp
            .build("multiple-e2e-urls.csv", resultsFile)
            .unsafeRunSync()

          val results = StreamingAppSpec.readResults(resultsFile).compile.toList.unsafeRunSync
          val expected =
            List(
              TestExtractedUrlResult("http://127.0.0.1:8090/test-api/html-page")
            )
          results mustBe expected
        }
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
