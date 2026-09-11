package no.fdk.catalogbackend.core.model

@JvmInline
value class ResourceType(val key: String) {
    override fun toString(): String = key
}
