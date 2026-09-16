package no.fdk.catalogbackend.core.publication

import no.fdk.catalogbackend.config.ApplicationProperties
import no.fdk.catalogbackend.core.model.ResourceType
import no.fdk.catalogbackend.core.persistence.CatalogResourceEntity
import no.fdk.catalogbackend.core.spi.ResourceRegistry
import no.fdk.catalogbackend.exception.BadRequestException
import no.fdk.catalogbackend.exception.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class PublicationService(
    private val registry: ResourceRegistry,
    private val applicationProperties: ApplicationProperties,
    private val harvestAdminClient: HarvestAdminClient,
) {
    @Transactional
    fun publish(resourceType: ResourceType, catalogId: String, id: String): CatalogResourceEntity {
        val store = registry.store(resourceType)
        val entity = store.findById(catalogId, id)
            ?: throw NotFoundException("No $resourceType with id $id in catalog $catalogId")

        if (entity.published) {
            throw BadRequestException("Resource $id in catalog $catalogId is already published")
        }

        // Evaluate before saving published=true — otherwise every publish looks like a first publish.
        val isFirstPublishInCatalog = !store.hasPublished(catalogId)
        val now = Instant.now()

        entity.published = true
        entity.publishedDate = now
        entity.lastModified = now
        val saved = store.save(entity)

        val metadata = registry.metadata(resourceType)
        val catalogUrl = catalogUrl(catalogId, metadata.pathSegment)
        if (isFirstPublishInCatalog) {
            harvestAdminClient.createNewDataSource(catalogId, metadata, catalogUrl)
        }
        harvestAdminClient.triggerHarvest(catalogId, metadata, catalogUrl)

        return saved
    }

    @Transactional
    fun unpublish(resourceType: ResourceType, catalogId: String, id: String): CatalogResourceEntity {
        val store = registry.store(resourceType)
        val entity = store.findById(catalogId, id)
            ?: throw NotFoundException("No $resourceType with id $id in catalog $catalogId")

        if (!entity.published) {
            throw BadRequestException("Resource $id in catalog $catalogId is not published")
        }

        entity.published = false
        entity.lastModified = Instant.now()
        val saved = store.save(entity)

        triggerHarvest(resourceType, catalogId)

        return saved
    }

    /** Re-trigger harvest after a patch on an already-published resource. */
    fun triggerHarvestIfPublished(resourceType: ResourceType, catalogId: String, published: Boolean) {
        if (published) {
            triggerHarvest(resourceType, catalogId)
        }
    }

    private fun triggerHarvest(resourceType: ResourceType, catalogId: String) {
        val metadata = registry.metadata(resourceType)
        harvestAdminClient.triggerHarvest(catalogId, metadata, catalogUrl(catalogId, metadata.pathSegment))
    }

    /** Same path the RDF controller serves for one catalog: `GET /graphs/catalogs/{catalogId}/{pathSegment}`. */
    private fun catalogUrl(catalogId: String, pathSegment: String): String =
        "${applicationProperties.catalogBackendUri}/graphs/catalogs/$catalogId/$pathSegment"
}
