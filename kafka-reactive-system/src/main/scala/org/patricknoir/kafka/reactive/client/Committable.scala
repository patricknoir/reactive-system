package org.patricknoir.kafka.reactive.client

import cats.data.OptionT
import cats.instances.all._
import cats.syntax.all._
import cats.{ Functor, Monad, Monoid }

import scala.annotation.tailrec
import scala.concurrent.{ ExecutionContext, Future }

/**
 * A Committable[T] type represents the
 * response from a Service which adopts message delivery at least once
 * as its semantic.
 * Created by patrick on 23/03/2017.
 */
sealed trait Committable[T] {

  val value: T
  val optionOffset: Option[Offset]
  def commit(): Future[Unit]

}

/**
 * Map of (topicName, partitionNumber) -> Position
 * @param positions
 */
case class Offset(positions: Map[(String, Int), Int])

object Offset {

  implicit val offsetMonoid = new Monoid[Offset] {
    override val empty: Offset = Offset(Map.empty)
    override def combine(offset1: Offset, offset2: Offset) = {
      val mergedMap = offset1.positions.mapValues(List(_)) |+| offset2.positions.mapValues(List(_))
      Offset(mergedMap.mapValues(_.max))
    }
  }
}

trait Committer {
  def commit(offset: Offset): Future[Unit]
}

object Committable {
  //make this private
  case class CommittableImpl[T](value: T, optionOffset: Option[Offset], optionCommitter: Option[Committer]) extends Committable[T] {
    override def commit() = {
      val result: Option[Future[Unit]] = for {
        offset <- optionOffset
        committer <- optionCommitter
      } yield committer.commit(offset)

      result.getOrElse(Future.successful[Unit](()))
    }
  }

  implicit def committableMonad(implicit optionCommitter: Option[Committer]) = new Monad[Committable] {
    override def pure[A](a: A) = CommittableImpl(a, None, optionCommitter)

    override def flatMap[A, B](ca: Committable[A])(f: A => Committable[B]): Committable[B] = {
      val cb = f(ca.value)
      val mergedOffset = for {
        offsetA <- ca.optionOffset
        offsetB <- cb.optionOffset
      } yield (offsetA |+| offsetB)
      CommittableImpl(cb.value, optionOffset = mergedOffset, optionCommitter)
    }

    override def tailRecM[A, B](a: A)(f: (A) => Committable[Either[A, B]]): Committable[B] = {

      @tailrec
      def tailRecMUtil(x: A, optionOffset: Option[Offset]): Committable[B] = f(x) match {
        case c @ CommittableImpl(Right(b), _, _) => c.copy(value = b)
        case c @ CommittableImpl(Left(a), optOffset, _) =>
          tailRecMUtil(a, optionOffset |+| optOffset)
      }

      tailRecMUtil(a, None)
    }
  }

  case class CommittableT[F[_]: Monad, A](run: F[Committable[A]])(implicit optionCommitter: Option[Committer]) {
    def map[B](f: A => B): CommittableT[F, B] = CommittableT(run.map[Committable[B]] { ca =>
      CommittableImpl(f(ca.value), ca.optionOffset, optionCommitter)
    })

    def flatMap[B](f: A => CommittableT[F, B]): CommittableT[F, B] =
      CommittableT(run.flatMap[Committable[B]] { fa =>
        f(fa.value).run
      })

    def value: F[A] = run.map(_.value)
    def optionOffset: F[Option[Offset]] = run.map(_.optionOffset)
    def commit(): F[Future[Unit]] = run.map(_.commit())
  }

  trait example[Credentials, Session, Account] {

    implicit val ec: ExecutionContext = ???
    implicit val optionCommitter: Option[Committer] = ???

    def req[In, Out](id: String)(in: In): Future[Out] = ???
    def reqCommit[In, Out](id: String)(in: In): Future[Committable[Out]] = ???

    val credentials: Credentials = ???

    val accInfo: Future[Account] = for {
      session <- req[Credentials, Session]("login")(credentials)
      account <- req[Session, Account]("info")(session)
    } yield account

    accInfo.map { account =>
      //do something with account
      println(account)
    }

    val accInfoCommittable: Future[Committable[Account]] = (for {
      session <- CommittableT(reqCommit[Credentials, Session]("login")(credentials))
      account <- CommittableT(reqCommit[Session, Account]("info")(session))
    } yield account).run

    accInfoCommittable.map { committableAccount =>
      //do something with account
      println(committableAccount.value)
      //commit offset once for both requests (credentials->session, session->account)
      committableAccount.commit()
    }
  }

}

