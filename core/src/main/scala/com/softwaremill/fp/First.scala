package com.softwaremill.fp

import java.time.Instant
import java.util.UUID

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import cats._
import cats.implicits._
import cats.effect._
import cats.effect.implicits._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object First extends App {
  case class Job(id: UUID, content: String)

  //

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  //

  def deleteAll(): ConnectionIO[Unit] = sql"DELETE FROM jobs".update.run.void

  def insert(content: String): ConnectionIO[Unit] =
    sql"""INSERT INTO jobs(id, content, next_delivery)
         |VALUES (${UUID.randomUUID()}, $content, ${Instant.now()})""".stripMargin.update.run.void

  def receive: ConnectionIO[Option[Job]] = {
    val now = Instant.now()
    val nextDelivery = now.plusSeconds(100)
    val nextJob =
      sql"""SELECT id, content FROM jobs WHERE next_delivery <= $now FOR UPDATE LIMIT 1"""
        .query[Job]
        .option
    nextJob.flatMap {
      case None =>
        (None: Option[Job]).pure[ConnectionIO]
      case Some(job) =>
        sql"""UPDATE jobs SET next_delivery = $nextDelivery WHERE id = ${job.id}""".update.run
          .map(_ => (Some(job): Option[Job]))
    }
  }

  def delete(id: UUID): ConnectionIO[Unit] = sql"DELETE FROM jobs WHERE id = $id".update.run.void

  //

  val program1: ConnectionIO[Unit] = for {
    _ <- deleteAll()
    _ <- insert("job1")
    j <- receive
  } yield {
    println(j)
  }

  //

  def businessLogic(j: Job): IO[Unit] =
    for {
      _ <- IO(println(s"Starting job: $j"))
      _ <- IO.sleep(3.seconds)
      _ <- IO(println(s"Finished job: $j"))
    } yield ()

  class TestQueue(transactor: Transactor[IO]) { // refactor from method parameters
    def processOne(): IO[Boolean] = receive.transact(transactor).flatMap {
      case None => false.pure[IO]
      case Some(job) =>
        businessLogic(job).flatMap(_ => delete(job.id).transact(transactor)).map(_ => true)
    }

    def processContinuously(): IO[Unit] = {
      processOne().flatMap { result =>
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
    val processors = 4
    val insertAndProcess: IO[Unit] = for {
      f1 <- insertContinuously(1).start
      _ <- (1 to processors).toList.map(_ => processContinuously().start).sequence
      _ <- f1.join
    } yield ()
  }

  //

  val transactor: Transactor[IO] =
    Transactor.fromDriverManager[IO]("org.postgresql.Driver", "jdbc:postgresql:fp", "postgres", "")

//  val result1: IO[Unit] = program1.transact(transactor)
//  result1.unsafeRunSync()

  val result2 =
    deleteAll().transact(transactor).flatMap(_ => new TestQueue(transactor).insertAndProcess)
  result2.unsafeRunSync()
}
