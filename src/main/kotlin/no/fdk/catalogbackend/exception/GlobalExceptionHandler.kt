package no.fdk.catalogbackend.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import tools.jackson.core.JacksonException

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler
    fun handleNotFoundException(ex: NotFoundException): ProblemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message)

    @ExceptionHandler
    fun handleBadRequestException(ex: BadRequestException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message)

    @ExceptionHandler
    fun handleInternalServerErrorException(ex: InternalServerErrorException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.message)

    @ExceptionHandler
    fun handleJacksonException(ex: JacksonException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.originalMessage)

    @ExceptionHandler
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): ProblemDetail {
        val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Failed to validate content.")

        ex.bindingResult.fieldErrors
            .map { fieldError -> mapOf("field" to fieldError.field, "message" to fieldError.defaultMessage) }
            .also { problemDetail.setProperty("errors", it) }

        return problemDetail
    }
}
