package no.fdk.catalogbackend.resource.informationmodel

import no.fdk.catalogbackend.core.model.ContactPoint
import no.fdk.catalogbackend.core.model.LocalizedStrings
import no.fdk.catalogbackend.core.persistence.CatalogResourceEntity
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Tag("unit")
class InformationModelMapperTest {
    private val mapper = InformationModelMapper(jacksonObjectMapper())

    @Test
    fun `payload round trips through jsonb shaped map and back to dto`() {
        val values = InformationModelValues(
            title = LocalizedStrings(nb = "Model"),
            description = LocalizedStrings(nb = "Description of Model"),
            contactPoints = listOf(
                ContactPoint(
                    name = LocalizedStrings(nb = "Contact"),
                    email = "contact@modeltest.com",
                    telephone = "12345678",
                    url = "https://modeltest.com/contact",
                ),
            ),
            status = "http://publications.europa.eu/resource/authority/product-status/DEVELOPMENT",
        )

        val entity = object : CatalogResourceEntity() {}.apply {
            id = "model-1"
            catalogId = "910244132"
            published = false
            created = Instant.parse("2026-01-01T00:00:00Z")
            lastModified = Instant.parse("2026-01-02T00:00:00Z")
            uri = "http://example.com/model-1"
            data = mapper.toPayload(values)
        }

        val dto = mapper.toDto(entity)

        assertEquals("model-1", dto.id)
        assertEquals("910244132", dto.catalogId)
        assertEquals(LocalizedStrings(nb = "Model"), dto.title)
        assertEquals(LocalizedStrings(nb = "Description of Model"), dto.description)
        assertEquals(
            listOf(
                ContactPoint(
                    name = LocalizedStrings(nb = "Contact"),
                    email = "contact@modeltest.com",
                    telephone = "12345678",
                    url = "https://modeltest.com/contact",
                ),
            ),
            dto.contactPoints,
        )
        assertEquals("http://publications.europa.eu/resource/authority/product-status/DEVELOPMENT", dto.status)
        assertEquals(values, mapper.toValues(dto))
    }

    @Test
    fun `null payload becomes an empty values object`() {
        val entity = object : CatalogResourceEntity() {}.apply {
            id = "model-1"
            catalogId = "910244132"
            created = Instant.parse("2026-01-01T00:00:00Z")
            data = null
        }

        val dto = mapper.toDto(entity)

        assertNull(dto.title)
        assertNull(dto.description)
        assertNull(dto.contactPoints)
        assertNull(dto.status)
    }
}
