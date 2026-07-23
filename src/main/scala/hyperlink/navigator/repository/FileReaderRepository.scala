package hyperlink.navigator.repository

import cats.effect.IO
import fs2.{Stream, text}
import hyperlink.navigator.repository.FileReaderRepository.Url

trait FileReaderRepository {
  def getLines(path: String): Stream[IO, Url]
}

object FileReaderRepository {
  case class Url(value: String) extends AnyVal

  def apply(): FileReaderRepository = new FileReaderRepository {
    override def getLines(path: String): Stream[IO, Url] = {
      fs2.io
        .readClassLoaderResource[IO](path)
        .through(text.utf8.decode)
        .through(text.lines)
        .filter(_.nonEmpty)
        .map(Url(_))
    }
  }
}
