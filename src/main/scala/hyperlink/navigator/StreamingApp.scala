package hyperlink.navigator

import cats.effect.IO
import cats.effect.std.Queue
import fs2.io.file.{Files, Path}
import fs2.{Stream, text}
import hyperlink.navigator.domain.RawHtmlPage
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
    val boundedQueue      = Queue.bounded[IO, Option[RawHtmlPage]](30)
    val concurrentFetches = 20

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
        .parEvalMapUnordered(concurrentFetches)(url => urlService.fetch(url.uri).attempt)
        .flatMap {
          case Left(error) =>
            Stream.exec(IO.println(s"Could not fetch page for url, $error"))
          case Right(value) =>
            Stream.emit(value)
        }
        .evalMap(item => queue.offer(Option(item)))
        .onComplete(Stream.eval(queue.offer(None)))

      val consumer: Stream[IO, Nothing] =
        Stream
          .fromQueueNoneTerminated(queue = queue, limit = 5)
          .evalMap(item => IO(hyperlinkExtractorService.parse(item)))
          .flatMap {
            case Left(error) =>
              Stream.exec(IO.println(s"Could not parse page for url, $error"))
            case Right(value) =>
              Stream.emit(value)
          }
          .evalMap(item => IO(hyperlinkExtractorService.extract(item)))
          .evalTap(item => IO.println(item))
          .map(hyperlink =>
            s"${hyperlink.originalUri},${hyperlink.extractedHyperLinks.mkString(" | ")}"
          )
          .intersperse("\n")
          .chunkN(50)
          .map(_.toList.mkString(""))
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
