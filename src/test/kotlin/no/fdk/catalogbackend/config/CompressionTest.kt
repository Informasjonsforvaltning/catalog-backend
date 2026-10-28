package no.fdk.catalogbackend.config

import no.fdk.catalogbackend.testsupport.PostgresTestcontainer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Verifies, through the real embedded server, that responses over the configured
 * threshold are gzipped when the client accepts it and left alone otherwise.
 */
@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestcontainer::class)
class CompressionTest {
    @LocalServerPort
    private var port = 0

    // java.net.http never decompresses on its own, so the raw bytes and headers reach the assertions.
    private val client = HttpClient.newHttpClient()

    private fun get(acceptEncoding: String?): HttpResponse<ByteArray> {
        val builder = HttpRequest.newBuilder(URI("http://localhost:$port/v3/api-docs")).GET()
        if (acceptEncoding != null) builder.header("Accept-Encoding", acceptEncoding)
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray())
    }

    @Test
    fun `gzips the response when the client accepts it`() {
        val response = get("gzip")
        assertEquals(200, response.statusCode())
        assertEquals("gzip", response.headers().firstValue("Content-Encoding").orElse(null))
        assertTrue(response.headers().allValues("Vary").joinToString(",").lowercase().contains("accept-encoding"))
        val body = response.body()
        assertTrue(body.size > 2 && body[0] == 0x1f.toByte() && body[1] == 0x8b.toByte(), "body is not gzip")
    }

    @Test
    fun `leaves the response uncompressed when the client does not ask`() {
        val response = get(null)
        assertEquals(200, response.statusCode())
        assertTrue(response.headers().firstValue("Content-Encoding").isEmpty)
        assertTrue(response.body().size > 1024, "test endpoint must exceed the 1 KB threshold")
    }
}
