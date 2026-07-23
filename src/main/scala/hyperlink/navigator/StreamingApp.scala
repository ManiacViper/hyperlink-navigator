package hyperlink.navigator

import cats.effect.IO
import fs2.io.file.{Files, Path}
import fs2.{Stream, text}
import hyperlink.navigator.domain.ValidatedUrl
import hyperlink.navigator.repository.FileReaderRepository
import hyperlink.navigator.service.InputValidatorService

object StreamingApp {
  def stream(
    inputFilePath: String,
    resultsFilePath: String,
    fileReaderRepository: FileReaderRepository,
    inputValidatorService: InputValidatorService
  ): IO[Unit] = {
    val fileReadingStream: Stream[IO, ValidatedUrl] = fileReaderRepository
      .getLines(inputFilePath)
      .map(rawData => inputValidatorService.validateRow(rawData.value))
      .flatMap {
        case Left(error) =>
          Stream.exec(IO.println(s"File error=$error"))
        case Right(value) =>
          Stream.emit(value)
      }

    fileReadingStream
      .map(_.uri.toString)
      .intersperse("\n")
      .through(text.utf8.encode)
      .through(Files[IO].writeAll(Path(resultsFilePath)))
      .compile
      .drain
  }

}
