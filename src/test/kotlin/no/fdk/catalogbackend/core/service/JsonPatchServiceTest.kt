package no.fdk.catalogbackend.core.service

import no.fdk.catalogbackend.core.model.JsonPatchOperation
import no.fdk.catalogbackend.core.model.OpEnum
import no.fdk.catalogbackend.exception.BadRequestException
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Tag("unit")
class JsonPatchServiceTest {
    private val service = JsonPatchService(jacksonObjectMapper())

    private data class Target(val id: String, val title: String?, val identifier: String? = null, val keywords: List<String> = emptyList())

    private val original = Target(id = "resource-1", title = "Original", keywords = listOf("a"))

    @Test
    fun `replace updates a mutable field`() {
        val patched = service.patch(original, listOf(JsonPatchOperation(OpEnum.REPLACE, "/title", "Updated")))

        assertEquals(Target(id = "resource-1", title = "Updated", keywords = listOf("a")), patched)
    }

    @Test
    fun `add appends to a list`() {
        val patched = service.patch(original, listOf(JsonPatchOperation(OpEnum.ADD, "/keywords/-", "b")))

        assertEquals(listOf("a", "b"), patched.keywords)
    }

    @Test
    fun `remove clears a nullable field`() {
        val patched = service.patch(original, listOf(JsonPatchOperation(OpEnum.REMOVE, "/title")))

        assertEquals(null, patched.title)
    }

    @ParameterizedTest
    @ValueSource(strings = ["/id", "/catalogId", "/created", "/lastModified", "/published", "/publishedDate", "/uri"])
    fun `operations on server-owned paths are rejected`(path: String) {
        val exception = assertFailsWith<BadRequestException> {
            service.patch(original, listOf(JsonPatchOperation(OpEnum.REPLACE, path, "anything")))
        }

        assertEquals("Patch operations on immutable path(s) $path are not allowed.", exception.message)
    }

    @Test
    fun `nested paths below a server-owned path are rejected`() {
        assertFailsWith<BadRequestException> {
            service.patch(original, listOf(JsonPatchOperation(OpEnum.REPLACE, "/uri/host", "example.com")))
        }
    }

    @Test
    fun `paths that only share a prefix with a server-owned path are allowed`() {
        val patched = service.patch(original, listOf(JsonPatchOperation(OpEnum.REPLACE, "/identifier", "urn:x")))

        assertEquals("urn:x", patched.identifier)
    }

    @Test
    fun `the source of a move is checked as well as its target`() {
        assertFailsWith<BadRequestException> {
            service.patch(original, listOf(JsonPatchOperation(OpEnum.MOVE, "/title", from = "/id")))
        }
    }

    @Test
    fun `an empty patch is rejected rather than silently doing nothing`() {
        assertFailsWith<BadRequestException> { service.patch(original, emptyList()) }
    }

    @Test
    fun `a patch against a missing path fails as a bad request`() {
        assertFailsWith<BadRequestException> {
            service.patch(original, listOf(JsonPatchOperation(OpEnum.REPLACE, "/nonexistent", "x")))
        }
    }
}
