import cats.effect.IO
import fs2.{INothing, Stream}

class ExtendedStream [F[t], O] (stream: Stream[F, O]) {

  // Repeats a stream indefinitely
  def repeat_(): Stream[F, O] = stream ++ repeat_

  // strips all output from a stream
  def drain_(): Stream[F, INothing] = stream.flatMap(_ => Stream.empty)

  // catches any errors produced by a stream
  def attempt_(): Stream[F, Either[O, Throwable]] =
    stream
      .map(Left(_))
      .handleErrorWith { e =>
        Stream(Right(e))
      }
}

object ExtendedStream {
  implicit def convertFromStream[F[t], O](stream: Stream[F, O]): ExtendedStream[F, O] =
    new ExtendedStream[F, O](stream)

  // runs an effect and ignores its output
  def eval__[F[_],O](e: F[O]): Stream[F, INothing] = Stream.eval(e).flatMap {_ => Stream.empty }
}

object StreamBuildingApp extends App {

  import ExtendedStream._

  assert(Stream(1, 0).repeat_.take(6).toList == List(1, 0, 1, 0, 1, 0))
  assert(Stream(1, 2, 3).drain_.toList == List())

  // !!
  ExtendedStream.eval__(IO(println("!!"))).compile.toVector.unsafeRunSync()

  // List(Right(1), Right(2), Left(java.lang.Exception: nooo!!!))
  println((Stream(1, 2) ++ Stream(3).map(_ => throw new Exception("nooo!!!"))).attempt.toList)
}
