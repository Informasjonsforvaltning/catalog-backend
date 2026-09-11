package no.fdk.catalogbackend.core.service

import jakarta.json.Json
import no.fdk.catalogbackend.core.model.JsonPatchOperation
import no.fdk.catalogbackend.exception.BadRequestException
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.io.StringReader

@Service
class JsonPatchService(private val objectMapper: ObjectMapper) {
    fun <T : Any> patch(original: T, operations: List<JsonPatchOperation>, clazz: Class<T>): T {
        if (operations.isEmpty()) throw BadRequestException("Patch document is empty.")

        rejectImmutablePaths(operations)

        return try {
            val patch = Json.createReader(StringReader(objectMapper.writeValueAsString(operations))).readArray()
            val target = Json.createReader(StringReader(objectMapper.writeValueAsString(original))).readObject()

            objectMapper.readValue(Json.createPatch(patch).apply(target).toString(), clazz)
        } catch (ex: Exception) {
            throw BadRequestException("Failed to apply patch: ${ex.message}")
        }
    }

    private fun rejectImmutablePaths(operations: List<JsonPatchOperation>) {
        val violations = operations
            .flatMap { listOfNotNull(it.path, it.from) }
            .filter { path -> IMMUTABLE_PATHS.any { path == it || path.startsWith("$it/") } }
            .distinct()

        if (violations.isNotEmpty()) {
            throw BadRequestException("Patch operations on immutable path(s) ${violations.joinToString()} are not allowed.")
        }
    }

    companion object {
        val IMMUTABLE_PATHS = setOf("/id", "/catalogId", "/created", "/lastModified", "/published", "/publishedDate", "/uri")
    }
}

inline fun <reified T : Any> JsonPatchService.patch(original: T, operations: List<JsonPatchOperation>): T =
    patch(original, operations, T::class.java)
