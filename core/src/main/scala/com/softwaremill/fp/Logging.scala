package com.softwaremill.fp

import com.typesafe.scalalogging.StrictLogging
import doobie.util.log.{ExecFailure, LogHandler, ProcessingFailure, Success}

trait Logging extends StrictLogging {
  Thread.setDefaultUncaughtExceptionHandler((t, e) => logger.error("Uncaught exception in thread: " + t, e))

  implicit val doobieLogHandler: LogHandler = LogHandler {
    case Success(_, _, _, _) =>
    case ProcessingFailure(sql, args, exec, processing, failure) =>
      logger.error(
        s"Processing failure (execution: $exec, processing: $processing): $sql | args: $args",
        failure
      )
    case ExecFailure(sql, args, exec, failure) =>
      logger.error(s"Execution failure (execution: $exec): $sql | args: $args", failure)
  }
}
