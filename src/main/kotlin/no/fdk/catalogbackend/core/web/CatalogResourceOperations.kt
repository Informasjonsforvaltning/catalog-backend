package no.fdk.catalogbackend.core.web

import no.fdk.catalogbackend.core.model.JsonPatchOperation
import no.fdk.catalogbackend.core.model.ResourceType
import no.fdk.catalogbackend.core.service.CatalogResourceService
import no.fdk.catalogbackend.core.service.JsonPatchService
import no.fdk.catalogbackend.core.spi.ResourceMapper
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.net.URI

/**
 * Shared CRUD orchestration for resource controllers.
 */
@Component
class CatalogResourceOperations(private val service: CatalogResourceService, private val patchService: JsonPatchService) {
    fun <V : Any, D : Any> findAll(resourceType: ResourceType, catalogId: String, mapper: ResourceMapper<V, D>): List<D> =
        service.findAll(resourceType, catalogId).map(mapper::toDto)

    fun <V : Any, D : Any> findById(resourceType: ResourceType, catalogId: String, id: String, mapper: ResourceMapper<V, D>): D =
        mapper.toDto(service.findById(resourceType, catalogId, id))

    fun <V : Any, D : Any> register(
        resourceType: ResourceType,
        catalogId: String,
        pathSegment: String,
        values: V,
        mapper: ResourceMapper<V, D>,
        validate: (V) -> Unit = {},
    ): ResponseEntity<Void> {
        validate(values)
        val created = service.register(resourceType, catalogId, mapper.toPayload(values))
        return ResponseEntity.created(URI.create("/internal/catalogs/$catalogId/$pathSegment/${created.id}")).build()
    }

    fun <V : Any, D : Any> patch(
        resourceType: ResourceType,
        catalogId: String,
        id: String,
        operations: List<JsonPatchOperation>,
        mapper: ResourceMapper<V, D>,
        dtoClass: Class<D>,
        validate: (V) -> Unit = {},
    ): D {
        val current = mapper.toDto(service.findById(resourceType, catalogId, id))
        val patched = patchService.patch(current, operations, dtoClass)
        val values = mapper.toValues(patched)
        validate(values)
        return mapper.toDto(service.update(resourceType, catalogId, id, mapper.toPayload(values)))
    }

    fun delete(resourceType: ResourceType, catalogId: String, id: String) {
        service.delete(resourceType, catalogId, id)
    }
}
