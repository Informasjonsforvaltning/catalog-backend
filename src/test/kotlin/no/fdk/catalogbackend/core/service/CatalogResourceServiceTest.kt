package no.fdk.catalogbackend.core.service

import no.fdk.catalogbackend.core.model.ResourceType
import no.fdk.catalogbackend.exception.NotFoundException
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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest
@Import(PostgresTestcontainer::class)
@Transactional
class CatalogResourceServiceTest(@param:Autowired val service: CatalogResourceService) {
    private val catalogId = "910244132"
    private val otherCatalogId = "123456789"

    private fun register(catalog: String = catalogId, title: String = "Title") =
        service.register(FAKE_RESOURCE, catalog, mapOf("title" to title))

    @Test
    fun `register assigns an id, timestamps and a dereferenceable uri`() {
        val entity = register()

        assertTrue(entity.id.isNotBlank())
        assertEquals(catalogId, entity.catalogId)
        assertEquals(mapOf("title" to "Title"), entity.data)
        assertEquals(false, entity.published)
        assertNotNull(entity.lastModified)
        assertEquals("http://localhost:5050/catalogs/$catalogId/fake-resources/${entity.id}", entity.uri)
    }

    @Test
    fun `the stored payload survives a round trip through jsonb`() {
        val nested = mapOf("title" to "Title", "keywords" to listOf("a", "b"), "nested" to mapOf("depth" to 2))
        val id = service.register(FAKE_RESOURCE, catalogId, nested).id

        assertEquals(nested, service.findById(FAKE_RESOURCE, catalogId, id).data)
    }

    @Test
    fun `findById returns the registered resource`() {
        val id = register().id

        assertEquals(id, service.findById(FAKE_RESOURCE, catalogId, id).id)
    }

    @Test
    fun `findById in the wrong catalog reports not found rather than leaking the resource`() {
        val id = register().id

        assertFailsWith<NotFoundException> { service.findById(FAKE_RESOURCE, otherCatalogId, id) }
    }

    @Test
    fun `findAll is scoped to one catalog`() {
        register()
        register()
        register(catalog = otherCatalogId)

        assertEquals(2, service.findAll(FAKE_RESOURCE, catalogId).size)
        assertEquals(1, service.findAll(FAKE_RESOURCE, otherCatalogId).size)
    }

    @Test
    fun `update replaces the payload and advances lastModified`() {
        val original = register()
        val originalModified = original.lastModified

        val updated = service.update(FAKE_RESOURCE, catalogId, original.id, mapOf("title" to "Changed"))

        assertEquals(mapOf("title" to "Changed"), updated.data)
        assertEquals(original.created, updated.created)
        assertTrue(updated.lastModified!! >= originalModified!!)
    }

    @Test
    fun `update cannot reach another catalog's resource`() {
        val id = register().id

        assertFailsWith<NotFoundException> { service.update(FAKE_RESOURCE, otherCatalogId, id, mapOf("title" to "Hijacked")) }
    }

    @Test
    fun `delete removes the resource`() {
        val id = register().id

        service.delete(FAKE_RESOURCE, catalogId, id)

        assertFailsWith<NotFoundException> { service.findById(FAKE_RESOURCE, catalogId, id) }
    }

    @Test
    fun `delete cannot reach another catalog's resource`() {
        val id = register().id

        assertFailsWith<NotFoundException> { service.delete(FAKE_RESOURCE, otherCatalogId, id) }
    }

    @Test
    fun `counts are grouped per catalog and per type`() {
        register()
        register()
        register(catalog = otherCatalogId)

        val counts = service.countsPerCatalog(null)

        assertEquals(mapOf(catalogId to 2L, otherCatalogId to 1L), counts[FAKE_RESOURCE])
    }

    @Test
    fun `counts can be restricted to the catalogs a caller may see`() {
        register()
        register(catalog = otherCatalogId)

        assertEquals(mapOf(catalogId to 1L), service.countsPerCatalog(listOf(catalogId))[FAKE_RESOURCE])
    }

    @Test
    fun `an empty catalog filter counts nothing rather than everything`() {
        register()

        assertEquals(emptyMap(), service.countsPerCatalog(emptyList())[FAKE_RESOURCE])
    }

    @Test
    fun `an unregistered resource type is rejected`() {
        assertFailsWith<IllegalArgumentException> { service.findAll(ResourceType("UNREGISTERED"), catalogId) }
    }

    @Test
    fun `the store hands back the concrete entity type`() {
        assertTrue(register() is FakeResourceEntity)
    }
}
