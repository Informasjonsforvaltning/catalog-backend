package no.fdk.catalogbackend.core.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param

/**
 * Derived queries every resource type inherits, a type declares its repository by extending this with
 * its concrete entity.
 */
@NoRepositoryBean
interface CatalogResourceRepository<T : CatalogResourceEntity> : JpaRepository<T, String> {
    fun findAllByCatalogId(catalogId: String): List<T>

    fun findByIdAndCatalogId(id: String, catalogId: String): T?

    fun findAllByPublishedIsTrue(): List<T>

    fun findAllByCatalogIdAndPublishedIsTrue(catalogId: String): List<T>

    fun findByIdAndCatalogIdAndPublishedIsTrue(id: String, catalogId: String): T?

    fun existsByCatalogIdAndPublishedIsTrue(catalogId: String): Boolean

    @Query("SELECT e.catalogId AS catalogId, COUNT(e) AS total FROM #{#entityName} e GROUP BY e.catalogId")
    fun countsPerCatalog(): List<CatalogIdCount>

    @Query(
        "SELECT e.catalogId AS catalogId, COUNT(e) AS total FROM #{#entityName} e " +
            "WHERE e.catalogId IN :catalogIds GROUP BY e.catalogId",
    )
    fun countsPerCatalog(@Param("catalogIds") catalogIds: Collection<String>): List<CatalogIdCount>
}

/** Projection for the grouped count, so counting never loads rows into memory. */
interface CatalogIdCount {
    val catalogId: String
    val total: Long
}
