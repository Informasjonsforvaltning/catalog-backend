package no.fdk.catalogbackend.core.spi

import no.fdk.catalogbackend.core.model.ResourceType
import no.fdk.catalogbackend.core.persistence.CatalogResourceEntity

/**
 * Translates between a type's public shape and the opaque `jsonb` payload core stores.
 *
 * @param V the write payload accepted from clients
 * @param D the response shape, adding server-owned fields
 */
interface ResourceMapper<V : Any, D : Any> {
    val resourceType: ResourceType

    fun toPayload(values: V): Map<String, Any?>

    fun toDto(entity: CatalogResourceEntity): D

    /** Strips server-owned fields from a DTO. */
    fun toValues(dto: D): V
}
