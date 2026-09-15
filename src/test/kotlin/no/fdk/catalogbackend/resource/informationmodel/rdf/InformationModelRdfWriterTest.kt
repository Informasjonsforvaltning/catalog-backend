package no.fdk.catalogbackend.resource.informationmodel.rdf

import no.fdk.catalogbackend.config.ApplicationProperties
import no.fdk.catalogbackend.core.rdf.createRDFResponse
import no.fdk.catalogbackend.core.service.ResourceUriService
import no.fdk.catalogbackend.resource.informationmodel.INFORMATION_MODEL
import no.fdk.catalogbackend.resource.informationmodel.InformationModelEntity
import no.fdk.catalogbackend.testsupport.checkIfIsomorphicAndPrintDiff
import no.fdk.catalogbackend.testsupport.loadTurtle
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.slf4j.LoggerFactory
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.io.StringReader
import kotlin.test.assertTrue

@Tag("unit")
class InformationModelRdfWriterTest {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val properties = ApplicationProperties(
        catalogIdentifierHost = "http://localhost:5050",
        informationModelIdentifierHost = "http://localhost:5050",
        organizationCatalogUri = "http://localhost:5050",
        harvestAdminUri = "http://localhost:5050",
    )
    private val resourceUriService = mock<ResourceUriService> {
        on { catalogUri(INFORMATION_MODEL, "910244132") } doReturn
            "http://localhost:5050/catalogs/910244132/information-models"
        on { resourceUri(INFORMATION_MODEL, "model-1") } doReturn
            "http://localhost:5050/information-models/model-1"
    }
    private val writer = InformationModelRdfWriter(resourceUriService, properties, jacksonObjectMapper())

    @Test
    fun `serialised model is isomorphic with the golden turtle fixture`() {
        val entity = InformationModelEntity().apply {
            id = "model-1"
            catalogId = "910244132"
            uri = "http://localhost:5050/information-models/model-1"
            data = mapOf(
                "title" to mapOf("nb" to "Testmodell"),
                "description" to mapOf("nb" to "En beskrivelse"),
                "contactPoints" to listOf(
                    mapOf(
                        "name" to mapOf("nb" to "Kontakt"),
                        "email" to "kontakt@example.com",
                        "telephone" to "+47 12 34 56 78",
                    ),
                ),
            )
        }

        val actual = ModelFactory.createDefaultModel()
        writer.write(actual, entity)

        assertTrue(checkIfIsomorphicAndPrintDiff(actual, loadTurtle("rdf/information_model.ttl"), "information_model", logger))
    }

    @Test
    fun `serialisation is isomorphic across rdf formats`() {
        val entity = InformationModelEntity().apply {
            id = "model-1"
            catalogId = "910244132"
            uri = "http://localhost:5050/information-models/model-1"
            data = mapOf("title" to mapOf("nb" to "Testmodell"))
        }

        val turtleModel = ModelFactory.createDefaultModel()
        writer.write(turtleModel, entity)
        val turtle = turtleModel.createRDFResponse(Lang.TURTLE)

        listOf(Lang.RDFXML, Lang.JSONLD, Lang.NTRIPLES).forEach { lang ->
            val roundTripped = ModelFactory.createDefaultModel().read(StringReader(turtleModel.createRDFResponse(lang)), null, lang.name)
            val expected = ModelFactory.createDefaultModel().read(StringReader(turtle), null, "TURTLE")
            assertTrue(
                checkIfIsomorphicAndPrintDiff(roundTripped, expected, lang.name, logger),
                "format ${lang.name} must stay isomorphic with turtle",
            )
        }
    }
}
