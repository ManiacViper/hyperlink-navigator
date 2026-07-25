package hyperlink.navigator

import cats.effect.IO
import cats.effect.std.Queue
import fs2.io.file.{Files, Path}
import fs2.{Stream, text}
import hyperlink.navigator.domain.HtmlPage
import hyperlink.navigator.http.HttpClient
import hyperlink.navigator.repository.FileReaderRepository
import hyperlink.navigator.service.{HyperlinkExtractorService, InputValidatorService, UrlService}

object StreamingApp {
  private def stream(
    inputFilePath: String,
    resultsFilePath: String,
    fileReaderRepository: FileReaderRepository,
    inputValidatorService: InputValidatorService,
    urlService: UrlService,
    hyperlinkExtractorService: HyperlinkExtractorService
  ): IO[Unit] = {
    val boundedQueue = Queue.bounded[IO, Option[HtmlPage]](30)

    boundedQueue.flatMap { queue =>
      val producer: Stream[IO, Unit] = fileReaderRepository
        .getLines(inputFilePath)
        .map(rawData => inputValidatorService.validateRow(rawData.value))
        .flatMap {
          case Left(error) =>
            Stream.exec(IO.println(s"Input Url error=$error"))
          case Right(value) =>
            Stream.emit(value)
        }
        .evalMap(url => urlService.fetch(url.uri).attempt)
        .flatMap {
          case Left(error) =>
            Stream.exec(IO.println(s"Could not fetch url=$error"))
          case Right(value) =>
            Stream.emit(value)
        }
        .evalMap(item => queue.offer(Option(item)))
        .onComplete(Stream.eval(queue.offer(None)))

      val consumer: Stream[IO, Nothing] =
        Stream
          .fromQueueNoneTerminated(queue = queue, limit = 5)
          .evalMap(item => IO(hyperlinkExtractorService.extract(item)))
          .evalTap(item => IO.println(item))
          .map(hyperlink =>
            s"${hyperlink.originalUri},${hyperlink.extractedHyperLinks.mkString(" | ")}"
          )
          .intersperse("\n")
          .through(text.utf8.encode)
          .through(Files[IO].writeAll(Path(resultsFilePath)))

      consumer.concurrently(producer).compile.drain
    }
  }

  def build(inputUrlsPath: String, extractedUrlsPath: String): IO[Unit] = {
    HttpClient().use { client =>
      StreamingApp.stream(
        inputUrlsPath,
        extractedUrlsPath,
        FileReaderRepository(),
        InputValidatorService(),
        UrlService(client),
        HyperlinkExtractorService()
      )
    }
  }

}
