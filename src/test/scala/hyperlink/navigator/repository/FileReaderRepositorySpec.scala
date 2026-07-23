package hyperlink.navigator.repository

import cats.effect.unsafe.implicits.global
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec

class FileReaderRepositorySpec extends AnyWordSpec {
  "FileReaderRepository" should {
    "read all urls in the file" when {
      "there are urls" in {
        val fileName   = "test-urls.csv"
        val fileReader = FileReaderRepository()
        val result = fileReader.getLines(fileName).compile.count.unsafeRunSync
        result mustBe 3
      }
    }

    "return error" when {
      "input file with urls is missing" in {
        val fileName   = "non-existing-file.csv"
        val fileReader = FileReaderRepository()
        val Left(result) = fileReader.getLines(fileName).compile.count.attempt.unsafeRunSync
        result.getMessage mustBe  "Resource non-existing-file.csv not found"
      }
    }
  }

}
