package no.fdk.catalogbackend.core.persistence

import no.fdk.catalogbackend.core.model.ResourceType
import no.fdk.catalogbackend.core.spi.ResourceStore
import kotlin.reflect.KClass

/**
 * Generic [ResourceStore] implementation. A resource type binds it to its own entity and repository:
 *
 * ```
 * @Component
 * class InformationModelStore(repository: InformationModelRepository) :
 *     JpaResourceStore<InformationModelEntity>(INFORMATION_MODEL, repository, InformationModelEntity::class, ::InformationModelEntity)
 * ```
 */
abstract class JpaResourceStore<T : CatalogResourceEntity>(
    override val resourceType: ResourceType,
    private val repository: CatalogResourceRepository<T>,
    private val entityType: KClass<T>,
    private val factory: () -> T,
) : ResourceStore {
    override fun findAll(catalogId: String): List<CatalogResourceEntity> = repository.findAllByCatalogId(catalogId)

    override fun findById(catalogId: String, id: String): CatalogResourceEntity? = repository.findByIdAndCatalogId(id, catalogId)

    override fun findAllPublished(): List<CatalogResourceEntity> = repository.findAllByPublishedIsTrue()

    override fun findAllPublished(catalogId: String): List<CatalogResourceEntity> =
        repository.findAllByCatalogIdAndPublishedIsTrue(catalogId)

    override fun findPublishedById(id: String): CatalogResourceEntity? = repository.findByIdAndPublishedIsTrue(id)

    override fun save(entity: CatalogResourceEntity): CatalogResourceEntity = repository.save(entity.narrow())

    override fun delete(entity: CatalogResourceEntity) = repository.delete(entity.narrow())

    override fun hasPublished(catalogId: String): Boolean = repository.existsByCatalogIdAndPublishedIsTrue(catalogId)

    override fun countsPerCatalog(catalogIds: Collection<String>?): Map<String, Long> {
        val counts = when {
            catalogIds == null -> repository.countsPerCatalog()
            catalogIds.isEmpty() -> emptyList()
            else -> repository.countsPerCatalog(catalogIds)
        }
        return counts.associate { it.catalogId to it.total }
    }

    override fun newEntity(): CatalogResourceEntity = factory()

    @Suppress("UNCHECKED_CAST")
    private fun CatalogResourceEntity.narrow(): T = if (entityType.isInstance(this)) {
        this as T
    } else {
        throw IllegalArgumentException(
            "Resource type $resourceType is backed by ${entityType.simpleName} but was handed a ${this::class.simpleName}",
        )
    }
}
