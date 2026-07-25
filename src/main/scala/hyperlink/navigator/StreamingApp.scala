package hyperlink.navigator

import cats.effect.IO
import cats.effect.std.Queue
import fs2.io.file.{Files, Path}
import fs2.{Stream, text}
import hyperlink.navigator.domain.{HtmlPage, ValidatedUrl}
import hyperlink.navigator.http.HttpClient
import hyperlink.navigator.repository.FileReaderRepository
import hyperlink.navigator.service.{InputValidatorService, UrlService}

object StreamingApp {
  private def stream(
    inputFilePath: String,
    resultsFilePath: String,
    fileReaderRepository: FileReaderRepository,
    inputValidatorService: InputValidatorService,
    urlService: UrlService
  ): IO[Unit] = {
    val boundedQueue = Queue.bounded[IO, Option[HtmlPage]](30)

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
        .evalMap(url => urlService.fetch(url.uri))
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

  def runStream = {
    (for {
      httpClient <- HttpClient()
      streamApp = StreamingApp.stream(
        "urls.csv",
        "extracted-urls.csv",
        FileReaderRepository(),
        InputValidatorService(),
        UrlService(httpClient)
      )
    } yield streamApp)
      .use(identity)
  }

}
