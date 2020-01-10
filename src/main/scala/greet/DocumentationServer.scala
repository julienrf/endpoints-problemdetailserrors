package greet

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import endpoints.akkahttp
import endpoints.openapi.model.OpenApi

// Endpoints serving the OpenAPI documentation
object DocumentationServer extends akkahttp.server.Endpoints with akkahttp.server.JsonEntitiesFromEncodersAndDecoders {

  val routes =
    endpoint(get(path / "documentation.json"), ok(jsonResponse[OpenApi]))
      .implementedBy(_ => GreetDocumentation.api) ~
   pathPrefix("assets" / Remaining) { file =>
      // optionally compresses the response with Gzip or Deflate
      // if the client accepts compressed responses
      encodeResponse {
        getFromResource("public/" + file)
      }
    } ~
    pathSingleSlash(redirect("/assets/swagger-ui/index.html", StatusCodes.SeeOther))

}
