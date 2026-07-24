package hyperlink.navigator

import cats.effect.IO
import cats.effect.std.Queue
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
    val boundedQueue = Queue.bounded[IO, Option[ValidatedUrl]](30)

    boundedQueue.flatMap { queue =>
      val producer: Stream[IO, Unit] = fileReaderRepository
        .getLines(inputFilePath)
        .map(rawData => inputValidatorService.validateRow(rawData.value))
        .flatMap {
          case Left(error) =>
            Stream.exec(IO.println(s"File error=$error"))
          case Right(value) =>
            Stream.emit(value)
        }
        .evalMap(item => queue.offer(Option(item)))
        .onComplete(Stream.eval(queue.offer(None)))

      val consumer: Stream[IO, Nothing] =
        Stream
          .fromQueueNoneTerminated(queue = queue, limit = 5)
          .evalTap(item => IO.println(item))
          .map(_.uri.toString)
          .intersperse("\n")
          .through(text.utf8.encode)
          .through(Files[IO].writeAll(Path(resultsFilePath)))

      consumer.concurrently(producer).compile.drain
    }
  }

}
