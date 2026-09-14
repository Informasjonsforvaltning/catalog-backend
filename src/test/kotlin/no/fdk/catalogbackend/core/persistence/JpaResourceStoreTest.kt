package no.fdk.catalogbackend.core.persistence

import no.fdk.catalogbackend.core.spi.ResourceRegistry
import no.fdk.catalogbackend.testsupport.PostgresTestcontainer
import no.fdk.catalogbackend.testsupport.fake.FAKE_RESOURCE
import no.fdk.catalogbackend.testsupport.fake.FakeResourceEntity
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest
@Import(PostgresTestcontainer::class)
@Transactional
class JpaResourceStoreTest(@param:Autowired val registry: ResourceRegistry) {
    private val store = { registry.store(FAKE_RESOURCE) }
    private val catalogId = "910244132"

    private fun persist(id: String, published: Boolean = false, catalog: String = catalogId) = store().save(
        store().newEntity().apply {
            this.id = id
            this.catalogId = catalog
            this.published = published
            this.created = Instant.now()
        },
    )

    @Test
    fun `newEntity produces the type's own entity`() {
        assertTrue(store().newEntity() is FakeResourceEntity)
    }

    @Test
    fun `a jsonb payload of null round trips as null`() {
        persist("resource-1")

        assertNull(store().findById(catalogId, "resource-1")?.data)
    }

    @Test
    fun `hasPublished is false until something is published in that catalog`() {
        persist("resource-1", published = false)

        assertFalse(store().hasPublished(catalogId))

        persist("resource-2", published = true)

        assertTrue(store().hasPublished(catalogId))
    }

    @Test
    fun `hasPublished is scoped to one catalog`() {
        persist("resource-1", published = true, catalog = "other")

        assertFalse(store().hasPublished(catalogId))
    }

    @Test
    fun `counts are grouped without loading rows`() {
        persist("resource-1")
        persist("resource-2")
        persist("resource-3", catalog = "other")

        assertEquals(mapOf(catalogId to 2L, "other" to 1L), store().countsPerCatalog(null))
    }

    @Test
    fun `delete removes only the targeted row`() {
        persist("resource-1")
        val survivor = persist("resource-2")

        store().delete(store().findById(catalogId, "resource-1")!!)

        assertNull(store().findById(catalogId, "resource-1"))
        assertEquals(survivor.id, store().findById(catalogId, "resource-2")?.id)
    }

    /**
     * The one unchecked cast in the codebase lives in [JpaResourceStore]. If a store is handed an entity
     * belonging to another type it must say so, not produce a ClassCastException from somewhere deeper.
     */
    @Test
    fun `saving an entity belonging to another type is refused with a clear message`() {
        val foreign = object : CatalogResourceEntity() {}

        val exception = assertFailsWith<IllegalArgumentException> { store().save(foreign) }

        assertTrue(exception.message!!.contains("is backed by FakeResourceEntity"))
    }

    @Test
    fun `published queries only return published rows`() {
        persist("draft", published = false)
        persist("live", published = true)
        persist("other-live", published = true, catalog = "other")

        assertEquals(listOf("live", "other-live"), store().findAllPublished().map { it.id }.sorted())
        assertEquals(listOf("live"), store().findAllPublished(catalogId).map { it.id })
        assertEquals("live", store().findPublishedById(catalogId, "live")?.id)
        assertNull(store().findPublishedById(catalogId, "draft"))
    }
}
