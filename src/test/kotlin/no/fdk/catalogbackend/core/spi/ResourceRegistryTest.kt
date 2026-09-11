package no.fdk.catalogbackend.core.spi

import no.fdk.catalogbackend.core.model.ResourceType
import no.fdk.catalogbackend.core.persistence.CatalogResourceEntity
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Tag("unit")
class ResourceRegistryTest {
    private val typeA = ResourceType("A")
    private val typeB = ResourceType("B")

    private fun store(type: ResourceType) = mock<ResourceStore> { on { resourceType } doReturn type }

    private fun metadata(type: ResourceType) = mock<ResourceTypeMetadata> { on { resourceType } doReturn type }

    private fun mapper(type: ResourceType) = mock<ResourceMapper<Any, Any>> { on { resourceType } doReturn type }

    @Test
    fun `a fully registered type is resolvable`() {
        val registry = ResourceRegistry(listOf(store(typeA)), listOf(metadata(typeA)), listOf(mapper(typeA)))

        assertEquals(listOf(typeA), registry.resourceTypes)
        assertEquals(typeA, registry.store(typeA).resourceType)
        assertEquals(typeA, registry.metadata(typeA).resourceType)
        assertEquals(typeA, registry.mapper(typeA).resourceType)
    }

    @Test
    fun `a type missing its mapper fails startup and names what is missing`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            ResourceRegistry(listOf(store(typeA)), listOf(metadata(typeA)), emptyList())
        }

        assertEquals("Incomplete resource type registration: A is missing mapper", exception.message)
    }

    @Test
    fun `a type contributing only a mapper fails startup`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            ResourceRegistry(emptyList(), emptyList(), listOf(mapper(typeA)))
        }

        assertTrue(exception.message!!.contains("A is missing store, metadata"))
    }

    @Test
    fun `two types claiming the same key fail startup`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            ResourceRegistry(listOf(store(typeA), store(typeA)), listOf(metadata(typeA)), listOf(mapper(typeA)))
        }

        assertEquals("More than one store registered for resource type(s) A", exception.message)
    }

    @Test
    fun `an unregistered type is rejected at lookup`() {
        val registry = ResourceRegistry(listOf(store(typeA)), listOf(metadata(typeA)), listOf(mapper(typeA)))

        assertFailsWith<IllegalArgumentException> { registry.store(typeB) }
    }

    @Test
    fun `no registered types at all is allowed`() {
        val registry = ResourceRegistry(emptyList(), emptyList(), emptyList())

        assertTrue(registry.resourceTypes.isEmpty())
        assertTrue(registry.stores().isEmpty())
    }

    @Test
    fun `stores are exposed for fan-out across every type`() {
        val entity = mock<CatalogResourceEntity>()
        val storeA = mock<ResourceStore> {
            on { resourceType } doReturn typeA
            on { findAll("catalog") } doReturn listOf(entity)
        }
        val registry = ResourceRegistry(
            listOf(storeA, store(typeB)),
            listOf(metadata(typeA), metadata(typeB)),
            listOf(mapper(typeA), mapper(typeB)),
        )

        assertEquals(listOf(typeA, typeB), registry.resourceTypes)
        assertEquals(2, registry.stores().size)
    }
}
