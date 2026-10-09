package no.fdk.catalogbackend.core.rdf

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.vocabulary.DCTerms
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Tag("unit")
class SafeAddLinkedPropertyTest {
    @Test
    fun `valid absolute uri is added as a linked resource`() {
        val model = ModelFactory.createDefaultModel()
        val resource = model.createResource("http://example.com/model")

        resource.safeAddLinkedProperty(DCTerms.creator, "https://example.com/organizations/910244132")

        assertTrue(resource.hasProperty(DCTerms.creator, model.createResource("https://example.com/organizations/910244132")))
    }

    @Test
    fun `null empty and free-text values are skipped`() {
        val model = ModelFactory.createDefaultModel()
        val resource = model.createResource("http://example.com/model")

        resource
            .safeAddLinkedProperty(DCTerms.creator, null)
            .safeAddLinkedProperty(DCTerms.creator, "")
            .safeAddLinkedProperty(DCTerms.creator, "Ola Nordmann")
            .safeAddLinkedProperty(DCTerms.creator, "not a uri")

        assertFalse(resource.hasProperty(DCTerms.creator))
        assertEquals(0, resource.listProperties(DCTerms.creator).toList().size)
    }

    @Test
    fun `safeAddLinkedProperties skips invalid entries`() {
        val model = ModelFactory.createDefaultModel()
        val resource = model.createResource("http://example.com/model")

        resource.safeAddLinkedProperties(
            DCTerms.subject,
            listOf("https://example.com/concepts/1", "free text", "", "https://example.com/concepts/2"),
        )

        assertTrue(resource.hasProperty(DCTerms.subject, model.createResource("https://example.com/concepts/1")))
        assertTrue(resource.hasProperty(DCTerms.subject, model.createResource("https://example.com/concepts/2")))
        assertEquals(2, resource.listProperties(DCTerms.subject).toList().size)
    }
}
