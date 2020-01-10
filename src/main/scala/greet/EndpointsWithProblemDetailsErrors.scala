package greet

import endpoints.{Invalid, algebra}

// Type to use to model client errors
case class ValidationError(detail: String)

// Alternate algebra that uses our custom error type
trait EndpointsWithProblemDetailsErrors
  extends algebra.EndpointsWithCustomErrors
    with algebra.JsonEntitiesFromSchemas {

  // Model both technical and business client errors with `ValidationError`
  override type ClientErrors = ValidationError

  // JSON schema for “Problem Details” documents. See https://tools.ietf.org/html/rfc7807#section-3.1
  final def problemDetailsSchema(tpe: String, title: String): JsonSchema[String] =
    field("type")(literal(tpe)) zip
    field("title")(literal(title)) zip
    field[String]("detail")

  // Description of response entities carrying client errors: an entity of
  // type `application/problem+json` containing a “Problem Details” JSON object
  override def clientErrorsResponseEntity: ResponseEntity[ValidationError] = {
    val schema: JsonSchema[ValidationError] =
      problemDetailsSchema("https://developer.bestmile.com/docs/api/errors/validation_error", "Validation error")
        .xmap(ValidationError)(_.detail)
    problemResponse(schema)
  }

  // The definition of response entities with content-type `application/problem+json` is abstract here.
  // Its implementation will be provided by server, client, and documentation interpreters.
  def problemResponse[A](schema: JsonSchema[A]): ResponseEntity[A]

  // Convert technical errors to the `ValidationError` type
  override def invalidToClientErrors(invalid: Invalid): ValidationError =
    ValidationError(invalid.errors.mkString(". "))

  // Convert `ValidationError` into a technical error
  override def clientErrorsToInvalid(validationError: ValidationError): Invalid =
    Invalid(validationError.detail)

  // Model both technical and business server errors with `Throwable`
  override type ServerError = Throwable

  // Convert technical errors into `Throwable` (this is a no-op because technical errors are already modeled as `Throwable`)
  def throwableToServerError(throwable: Throwable): Throwable = throwable
  def serverErrorToThrowable(serverError: Throwable): Throwable = serverError

  def serverErrorResponseEntity: ResponseEntity[Throwable] = {
    val schema =
      problemDetailsSchema("https://developer.bestmile.com/docs/api/errors/internal_server_error", "Internal server error")
        .xmap[Throwable](new Exception(_))(_.getMessage)
    problemResponse(schema)
  }
}

// Server interpreter
import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model.{ HttpEntity, MediaTypes }
import endpoints.akkahttp

trait EndpointsWithProblemDetailsErrorsServer
  extends EndpointsWithProblemDetailsErrors
    with akkahttp.server.EndpointsWithCustomErrors
    with akkahttp.server.JsonEntitiesFromSchemas {

  override def problemResponse[A](schema: JsonSchema[A]): ResponseEntity[A] = {
    val mediaType = MediaTypes.`application/problem+json`
    Marshaller.withFixedContentType(mediaType) { value =>
      HttpEntity(mediaType, stringCodec(schema).encode(value))
    }
  }

}

// Documentation interpreter
import endpoints.openapi
import endpoints.openapi.model.MediaType

trait EndpointsWithProblemDetailsErrorsDocumentation
  extends EndpointsWithProblemDetailsErrors
    with openapi.EndpointsWithCustomErrors
    with openapi.JsonEntitiesFromSchemas {

  override def problemResponse[A](schema: JsonSchema[A]): Map[String, MediaType] =
    Map("application/problem+json" -> MediaType(Some(toSchema(schema.docs))))

}
