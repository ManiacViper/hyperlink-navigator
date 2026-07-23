package hyperlink.navigator

import cats.effect.IO
import fs2.io.file.{Files, Path}
import fs2.{Stream, text}
import hyperlink.navigator.repository.FileReaderRepository

object StreamingApp {
  def stream(
              inputFilePath: String,
              resultsFilePath: String,
              fileReaderRepository: FileReaderRepository,
            ): IO[Unit] = {
    val fileReadingStream: Stream[IO, String] =
      fileReaderRepository
      .getLines(inputFilePath)
      .map(_.value)
      .intersperse("\n")

    fileReadingStream
      .through(text.utf8.encode)
      .through(Files[IO].writeAll(Path(resultsFilePath)))
      .compile
      .drain
  }

}
