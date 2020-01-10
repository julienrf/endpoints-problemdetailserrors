package greet

import akka.actor.ActorSystem
import akka.http.scaladsl.Http

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object Main {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("server-system")

    val interface = "0.0.0.0"
    val port = 8000
    Http().bindAndHandle(Server.routes, interface, port).andThen {
      case Failure(exception) =>
        System.err.println(s"Unable to start the server: $exception")
        System.exit(1)
      case Success(binding) =>
        println(s"Server started at http://$interface:$port. Exit with Ctrl+C.")
        sys.addShutdownHook {
          println("Stopping server")
          Await.result(binding.terminate(hardDeadline = 10.seconds), 20.seconds)
        }
    }

  }

}
