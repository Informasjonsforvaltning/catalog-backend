package no.fdk.catalogbackend.core.rdf

import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Tag("unit")
class JenaLangFromAcceptHeaderTest {
    @Test
    fun `null accept defaults to turtle`() {
        assertEquals(Lang.TURTLE, jenaLangFromAcceptHeader(null))
    }

    @Test
    fun `json-ld is accepted`() {
        assertEquals(Lang.JSONLD, jenaLangFromAcceptHeader("application/ld+json"))
    }

    @Test
    fun `unknown accept yields 406`() {
        val exception = assertFailsWith<ResponseStatusException> {
            jenaLangFromAcceptHeader("application/json")
        }
        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.statusCode)
    }
}
