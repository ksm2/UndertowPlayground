package systems.moellers.undertow.error

import io.undertow.util.StatusCodes

open class HttpError(val statusCode: Int, val statusName: String, override val message: String) : Throwable("HTTP $statusCode $statusName: $message")
class NotFound(message: String) : HttpError(StatusCodes.NOT_FOUND, StatusCodes.NOT_FOUND_STRING, message)
class BadRequest(message: String) : HttpError(StatusCodes.BAD_REQUEST, StatusCodes.BAD_REQUEST_STRING, message)
class MethodNotAllowed(message: String) : HttpError(StatusCodes.METHOD_NOT_ALLOWED, StatusCodes.METHOD_NOT_ALLOWED_STRING, message)

data class HttpErrorMessage(val message: String, val statusCode: Int, val statusName: String) {
    constructor(error: HttpError): this(error.message, error.statusCode, error.statusName)
}
