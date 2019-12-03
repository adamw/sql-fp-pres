package com.softwaremill.fp

import java.util.UUID

import cats.implicits._
import cats.effect._
import doobie._
import doobie.implicits._
import io.getquill.{idiom => _, _}
import doobie.quill.DoobieContext

import scala.concurrent.ExecutionContext

object Third extends App with Logging {
  case class Person(id: UUID, firstName: String, age: Int)

  //

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  //

  val dc = new DoobieContext.Postgres(SnakeCase)
  import dc._

  def delete: ConnectionIO[Unit] = run(quote {
    query[Person].delete
  }).void

  def insert(p: Person): ConnectionIO[UUID] =
    run(quote {
      query[Person].insert(lift(p)).returning(_.id)
    })

  def findJohns: ConnectionIO[List[Person]] =
    run(quote {
      query[Person].filter(_.firstName == "John")
    })

  //

  val transactor: Transactor[IO] =
    Transactor.fromDriverManager[IO]("org.postgresql.Driver", "jdbc:postgresql:fp", "postgres", "")

  val result = for {
    _ <- insert(Person(UUID.randomUUID(), "John", 10))
    _ <- insert(Person(UUID.randomUUID(), "Mary", 11))
    _ <- insert(Person(UUID.randomUUID(), "John", 12))
    _ <- insert(Person(UUID.randomUUID(), "Adam", 13))
    r <- findJohns
  } yield r
  println(result.transact(transactor).unsafeRunSync())
}
