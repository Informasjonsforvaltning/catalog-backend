package no.fdk.catalogbackend.core.service

import no.fdk.catalogbackend.config.ApplicationProperties
import no.fdk.catalogbackend.core.model.ResourceType
import no.fdk.catalogbackend.core.persistence.CatalogResourceEntity
import no.fdk.catalogbackend.core.spi.ResourceRegistry
import no.fdk.catalogbackend.exception.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * CRUD for any resource type, working in entities and opaque `jsonb` payloads.
 */
@Service
class CatalogResourceService(private val registry: ResourceRegistry, private val applicationProperties: ApplicationProperties) {
    fun findAll(resourceType: ResourceType, catalogId: String): List<CatalogResourceEntity> =
        registry.store(resourceType).findAll(catalogId)

    fun findById(resourceType: ResourceType, catalogId: String, id: String): CatalogResourceEntity =
        registry.store(resourceType).findById(catalogId, id)
            ?: throw NotFoundException("No $resourceType with id $id in catalog $catalogId")

    @Transactional
    fun register(resourceType: ResourceType, catalogId: String, payload: Map<String, Any?>): CatalogResourceEntity {
        val store = registry.store(resourceType)
        val now = Instant.now()

        val entity = store.newEntity().apply {
            this.id = UUID.randomUUID().toString()
            this.catalogId = catalogId
            this.created = now
            this.lastModified = now
            this.data = payload
            this.uri = resourceUri(resourceType, catalogId, this.id)
        }

        return store.save(entity)
    }

    @Transactional
    fun update(resourceType: ResourceType, catalogId: String, id: String, payload: Map<String, Any?>): CatalogResourceEntity {
        val entity = findById(resourceType, catalogId, id).apply {
            this.data = payload
            this.lastModified = Instant.now()
        }

        return registry.store(resourceType).save(entity)
    }

    @Transactional
    fun delete(resourceType: ResourceType, catalogId: String, id: String) {
        registry.store(resourceType).delete(findById(resourceType, catalogId, id))
    }

    /**
     * Counts per catalog across every registered type, restricted to [catalogIds] when given.
     */
    fun countsPerCatalog(catalogIds: Collection<String>?): Map<ResourceType, Map<String, Long>> =
        registry.stores().associate { it.resourceType to it.countsPerCatalog(catalogIds) }

    /**
     * Mints the resource's public URI, which must match the path the RDF controller serves it on.
     */
    private fun resourceUri(resourceType: ResourceType, catalogId: String, id: String): String {
        val pathSegment = registry.metadata(resourceType).pathSegment
        return "${applicationProperties.catalogBackendUri}/catalogs/$catalogId/$pathSegment/$id"
    }
}
