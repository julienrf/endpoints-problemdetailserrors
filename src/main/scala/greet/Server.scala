package greet

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RejectionHandler

object Server extends EndpointsWithProblemDetailsErrorsServer {

  val rejectionHandler =
    RejectionHandler.newBuilder()
      // We also customize the Akka HTTP “not found” handler to return a ProblemDetails entity
      .handleNotFound {
        val schema = problemDetailsSchema("https://developer.bestmile.com/docs/api/errors/not_found", "Not found")
        response(NotFound, problemResponse(schema))("Resource not found")
      }
      .result()

  val routes =
    handleRejections(rejectionHandler) {
      GreetServer.route ~ DocumentationServer.routes
    }

}
