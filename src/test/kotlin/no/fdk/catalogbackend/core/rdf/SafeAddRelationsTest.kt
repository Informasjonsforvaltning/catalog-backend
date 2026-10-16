package no.fdk.catalogbackend.core.rdf

import no.fdk.catalogbackend.core.model.ResourceRelation
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.vocabulary.DCTerms
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Tag("unit")
class SafeAddRelationsTest {
    @Test
    fun `valid relation type and source are added as a linked property`() {
        val model = ModelFactory.createDefaultModel()
        val resource = model.createResource("http://example.com/model")

        resource.safeAddRelations(
            listOf(
                ResourceRelation(
                    type = DCTerms.replaces.uri,
                    source = "https://example.com/information-models/old",
                ),
            ),
        )

        assertTrue(
            resource.hasProperty(
                DCTerms.replaces,
                model.createResource("https://example.com/information-models/old"),
            ),
        )
    }

    @Test
    fun `invalid or incomplete relations are skipped without validating vocabulary`() {
        val model = ModelFactory.createDefaultModel()
        val resource = model.createResource("http://example.com/model")

        resource.safeAddRelations(
            listOf(
                ResourceRelation(type = "", source = "https://example.com/a"),
                ResourceRelation(type = DCTerms.hasPart.uri, source = ""),
                ResourceRelation(type = "not a uri", source = "https://example.com/a"),
                ResourceRelation(type = DCTerms.hasPart.uri, source = "free text"),
                ResourceRelation(
                    type = "https://example.com/custom-relation",
                    source = "https://example.com/information-models/other",
                ),
            ),
        )

        assertFalse(resource.hasProperty(DCTerms.hasPart))
        assertEquals(1, resource.listProperties().toList().size)
        assertTrue(
            resource.hasProperty(
                model.createProperty("https://example.com/custom-relation"),
                model.createResource("https://example.com/information-models/other"),
            ),
        )
    }
}
