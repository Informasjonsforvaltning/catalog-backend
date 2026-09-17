package no.fdk.catalogbackend.core.publication

import no.fdk.catalogbackend.core.model.ResourceType
import no.fdk.catalogbackend.core.persistence.CatalogResourceEntity
import no.fdk.catalogbackend.core.service.CatalogResourceService
import no.fdk.catalogbackend.core.spi.ResourceRegistry
import no.fdk.catalogbackend.exception.BadRequestException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class PublicationService(
    private val catalogResourceService: CatalogResourceService,
    private val registry: ResourceRegistry,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun publish(resourceType: ResourceType, catalogId: String, id: String): CatalogResourceEntity {
        val store = registry.store(resourceType)
        val entity = catalogResourceService.findById(resourceType, catalogId, id)

        if (entity.published) {
            throw BadRequestException("Resource $id in catalog $catalogId is already published")
        }

        // Evaluate before saving published=true — otherwise every publish looks like a first publish.
        val isFirstPublishInCatalog = !store.hasPublished(catalogId)
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)

        entity.published = true
        // Keep the original first-published timestamp across unpublish/republish cycles.
        if (entity.publishedDate == null) {
            entity.publishedDate = now
        }
        entity.lastModified = now
        val saved = store.save(entity)

        eventPublisher.publishEvent(
            HarvestCatalogEvent(
                resourceType = resourceType,
                catalogId = catalogId,
                createDataSource = isFirstPublishInCatalog,
            ),
        )

        return saved
    }

    @Transactional
    fun unpublish(resourceType: ResourceType, catalogId: String, id: String): CatalogResourceEntity {
        val store = registry.store(resourceType)
        val entity = catalogResourceService.findById(resourceType, catalogId, id)

        if (!entity.published) {
            throw BadRequestException("Resource $id in catalog $catalogId is not published")
        }

        entity.published = false
        entity.lastModified = Instant.now().truncatedTo(ChronoUnit.MICROS)
        val saved = store.save(entity)

        eventPublisher.publishEvent(HarvestCatalogEvent(resourceType = resourceType, catalogId = catalogId))

        return saved
    }

    /**
     * Request harvest after a patch on an already-published resource.
     * Transactional so [HarvestCatalogEvent] is registered and delivered AFTER_COMMIT
     * even when the preceding update has already committed.
     */
    @Transactional
    fun triggerHarvestIfPublished(resourceType: ResourceType, catalogId: String, published: Boolean) {
        if (published) {
            eventPublisher.publishEvent(HarvestCatalogEvent(resourceType = resourceType, catalogId = catalogId))
        }
    }
}
