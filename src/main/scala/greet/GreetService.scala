package greet

import endpoints.openapi.model.Info

// Service description

case class Greet(message: String)

trait GreetEndpoint extends EndpointsWithProblemDetailsErrors {

  val greet = endpoint(
    get(path / "greet" /? qs[String]("name")),
    badRequest().orElse(ok(jsonResponse[Greet]))
  )

  implicit lazy val greetSchema: JsonSchema[Greet] =
    field[String]("message").xmap(Greet)(_.message)

}


// Server implementation

object GreetServer extends GreetEndpoint with EndpointsWithProblemDetailsErrorsServer {

  val route = greet.implementedBy { name =>
    // Uncomment the following line to see how runtime exceptions are rendered by the server
    // throw new Exception("Something went wrong...")
    if (name == "Voldemort") Left(ValidationError("“Voldemort” is not a valid name"))
    else Right(Greet(s"Hello, $name!"))
  }

}


// Documentation

object GreetDocumentation extends GreetEndpoint with EndpointsWithProblemDetailsErrorsDocumentation {

  val api = openApi(Info("Greet service", "1.0.0"))(greet)

}
