package no.fdk.catalogbackend.core.spi

import no.fdk.catalogbackend.core.model.ResourceType
import no.fdk.catalogbackend.core.persistence.CatalogResourceEntity

/**
 * Storage port for one resource type.
 *
 * Non-generic on purpose. Core resolves stores from a registry, and `JpaRepository.save(T)` is
 * contravariant in `T`, so a `CatalogResourceRepository<out CatalogResourceEntity>` projection would
 * forbid saving. Erasing the entity type at this boundary keeps core free of unchecked casts.
 * The casts live only in [no.fdk.catalogbackend.core.persistence.JpaResourceStore].
 */
interface ResourceStore {
    val resourceType: ResourceType

    fun findAll(catalogId: String): List<CatalogResourceEntity>

    fun findById(catalogId: String, id: String): CatalogResourceEntity?

    fun findAllPublished(): List<CatalogResourceEntity>

    fun findAllPublished(catalogId: String): List<CatalogResourceEntity>

    fun findPublishedById(catalogId: String, id: String): CatalogResourceEntity?

    fun save(entity: CatalogResourceEntity): CatalogResourceEntity

    fun delete(entity: CatalogResourceEntity)

    /** Whether the catalog already has a published resource of this type. */
    fun hasPublished(catalogId: String): Boolean

    /** Resource count per catalog id, restricted to [catalogIds] when given, otherwise across all catalogs. */
    fun countsPerCatalog(catalogIds: Collection<String>?): Map<String, Long>

    /** Core cannot instantiate a concrete entity, so the type supplies the factory. */
    fun newEntity(): CatalogResourceEntity
}
