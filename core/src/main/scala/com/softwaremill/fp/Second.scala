package com.softwaremill.fp

import java.util.UUID

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Second extends App with Logging {
  case class Job2(id: UUID, content: String)

  //

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  //

  def deleteAll(): ConnectionIO[Unit] = sql"DELETE FROM jobs2".update.run.void

  def insert(content: String): ConnectionIO[Unit] =
    sql"""INSERT INTO jobs2(id, content)
         |VALUES (${UUID.randomUUID()}, $content)""".stripMargin.update.run.void

  def receive: ConnectionIO[Option[Job2]] = {
    sql"""SELECT id, content FROM jobs2 FOR UPDATE SKIP LOCKED LIMIT 1"""
      .query[Job2]
      .option
  }

  def delete(id: UUID): ConnectionIO[Unit] = sql"DELETE FROM jobs2 WHERE id = $id".update.run.void

  //

  def businessLogic(j: Job2): IO[Unit] =
    for {
      _ <- IO(println(s"Starting job: $j"))
      _ <- IO.sleep(3.seconds)
      _ <- IO(println(s"Finished job: $j"))
    } yield ()

  class TestQueue(transactor: Transactor[IO]) {
    def processOne(): ConnectionIO[Boolean] = receive.flatMap {
      case None => false.pure[ConnectionIO]
      case Some(job) =>
        businessLogic(job).to[ConnectionIO].flatMap(_ => delete(job.id)).map(_ => true)
    }

    def processContinuously(): IO[Unit] = {
      processOne().transact(transactor).flatMap { result =>
        val delay = if (result) IO.pure(()) else IO.sleep(1.second)
        delay.flatMap(_ => processContinuously())
      }
    }

    def insertContinuously(counter: Int): IO[Unit] = {
      insert(s"job_$counter")
        .transact(transactor)
        .flatMap(_ => IO.sleep(500.milliseconds))
        .flatMap(_ => insertContinuously(counter + 1))
    }

    // start with 1, then 4, manually added, add more later
    val processors = 6
    val insertAndProcess: IO[Unit] = for {
      f1 <- insertContinuously(1).start
      _ <- (1 to processors).toList.map(_ => processContinuously().start).sequence
      _ <- f1.join
    } yield ()
  }

  //

  val transactor: Transactor[IO] =
    Transactor.fromDriverManager[IO]("org.postgresql.Driver", "jdbc:postgresql:fp", "postgres", "")

  val result2 =
    deleteAll().transact(transactor).flatMap(_ => new TestQueue(transactor).insertAndProcess)
  result2.unsafeRunSync()
}
