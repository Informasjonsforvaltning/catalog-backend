package no.fdk.catalogbackend.exception

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import kotlin.test.assertEquals

@Tag("unit")
class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    @Test
    fun `not found maps to 404 with the message as detail`() {
        val problem = handler.handleNotFoundException(NotFoundException("no such thing"))

        assertEquals(404, problem.status)
        assertEquals("no such thing", problem.detail)
    }

    @Test
    fun `bad request maps to 400 with the message as detail`() {
        val problem = handler.handleBadRequestException(BadRequestException("bad input"))

        assertEquals(400, problem.status)
        assertEquals("bad input", problem.detail)
    }

    @Test
    fun `internal server error maps to 500`() {
        val problem = handler.handleInternalServerErrorException(InternalServerErrorException("boom"))

        assertEquals(500, problem.status)
        assertEquals("boom", problem.detail)
    }

    @Test
    fun `validation failures are reported as a list of field errors`() {
        val bindingResult = mock<BindingResult> {
            on { fieldErrors } doReturn listOf(FieldError("informationModel", "title", "Cannot be blank"))
        }
        val exception = mock<MethodArgumentNotValidException> {
            on { this.bindingResult } doReturn bindingResult
        }

        val problem = handler.handleMethodArgumentNotValidException(exception)

        assertEquals(400, problem.status)
        assertEquals("Failed to validate content.", problem.detail)
        assertEquals(
            listOf(mapOf("field" to "title", "message" to "Cannot be blank")),
            problem.properties?.get("errors"),
        )
    }
}
