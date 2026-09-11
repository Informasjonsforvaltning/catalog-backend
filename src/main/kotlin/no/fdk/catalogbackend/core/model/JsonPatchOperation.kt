package no.fdk.catalogbackend.core.model

import com.fasterxml.jackson.annotation.JsonValue
import jakarta.validation.constraints.NotBlank

/** A single RFC 6902 operation. Serialised shape matches the spec, so the list can be handed to jakarta.json as-is. */
data class JsonPatchOperation(val op: OpEnum, @field:NotBlank val path: String, val value: Any? = null, val from: String? = null)

enum class OpEnum(@get:JsonValue val value: String) {
    ADD("add"),
    REMOVE("remove"),
    REPLACE("replace"),
    MOVE("move"),
    COPY("copy"),
    TEST("test"),
}
