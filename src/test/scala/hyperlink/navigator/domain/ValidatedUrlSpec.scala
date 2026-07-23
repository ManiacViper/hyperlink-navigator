package hyperlink.navigator.domain

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.net.URI

class ValidatedUrlSpec extends AnyWordSpec with Matchers {

  "ValidatedUrl" should {
    "return validated url" when {
      "its a valid uri" in {
        val value = "https://wwww.google.com"
        val Right(result) = ValidatedUrl.from(value)
        result mustBe ValidatedUrl(new URI(value))
      }
    }

    "return error" when {
      "url is not complete" in {
        val value = "withoutscheme"
        val Left(result) = ValidatedUrl.from(value)
        result mustBe "invalid url, [withoutscheme]"
      }

      "url is invalid" in {
        val value = "invalid url"
        val Left(result) = ValidatedUrl.from(value)
        result mustBe "Illegal character in path at index 7: invalid url"
      }

      "there is empty url" in {
        val empty = ""
        val Left(result) = ValidatedUrl.from(empty)
        result mustBe "invalid url, []"
      }
    }
  }
}
