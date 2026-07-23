package hyperlink.navigator.service

import hyperlink.navigator.domain.ValidatedUrl


trait InputValidatorService {
  def validateRow(value: String): Either[String, ValidatedUrl]
}

object InputValidatorService {
  def apply() = new InputValidatorService {
    override def validateRow(value: String): Either[String, ValidatedUrl] =
      ValidatedUrl.from(value)
  }

}