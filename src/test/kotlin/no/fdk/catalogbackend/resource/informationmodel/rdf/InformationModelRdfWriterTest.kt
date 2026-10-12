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
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.XSD
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.slf4j.LoggerFactory
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.io.StringReader
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Tag("unit")
class InformationModelRdfWriterTest {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val properties = ApplicationProperties(
        resourceIdentifierHost = "http://localhost:5050",
        catalogBackendUri = "http://localhost:5050",
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
    fun `empty catalog is serialised with type and publisher`() {
        val actual = ModelFactory.createDefaultModel()
        writer.writeCatalog(actual, "910244132", emptyList())

        assertTrue(
            checkIfIsomorphicAndPrintDiff(
                actual,
                loadTurtle("rdf/empty_information_model_catalog.ttl"),
                "empty_information_model_catalog",
                logger,
            ),
        )
    }

    private fun sampleEntity() = InformationModelEntity().apply {
        id = "model-1"
        catalogId = "910244132"
        uri = "http://localhost:5050/information-models/model-1"
        data = mapOf(
            "title" to mapOf("nb" to "Testmodell"),
            "description" to mapOf("nb" to "En beskrivelse"),
            "status" to "http://publications.europa.eu/resource/authority/product-status/PRODUCTION",
            "homepage" to "https://example.com/models/testmodell",
            "version" to mapOf("major" to 1, "minor" to 0, "patch" to 0),
            "creator" to "https://example.com/organizations/910244132",
            "subjects" to listOf(
                "https://www.wikidata.org/entity/Q183560",
                "https://data.norge.no/concepts/68aea19b-eb20-41e4-8a7c-85b7e6451bc0",
            ),
            "modified" to "2026-03-15",
            "contactPoints" to listOf(
                mapOf(
                    "name" to mapOf("nb" to "Kontakt"),
                    "email" to "kontakt@example.com",
                    "telephone" to "+47 12 34 56 78",
                ),
            ),
        )
    }

    @Test
    fun `serialised model is isomorphic with the golden turtle fixture`() {
        val actual = ModelFactory.createDefaultModel()
        writer.write(actual, sampleEntity())

        assertTrue(checkIfIsomorphicAndPrintDiff(actual, loadTurtle("rdf/information_model.ttl"), "information_model", logger))
    }

    @Test
    fun `catalog with members is isomorphic with the golden turtle fixture`() {
        val entity = sampleEntity()
        val actual = ModelFactory.createDefaultModel()
        writer.writeCatalog(actual, entity.catalogId, listOf(entity))
        writer.write(actual, entity)

        assertTrue(
            checkIfIsomorphicAndPrintDiff(
                actual,
                loadTurtle("rdf/information_model_catalog.ttl"),
                "information_model_catalog",
                logger,
            ),
        )
    }

    @ParameterizedTest
    @CsvSource("1996, gYear", "1996-11, gYearMonth", "2026-03-15, date")
    fun `modified is written with the xsd type matching its precision`(value: String, xsdType: String) {
        val entity = InformationModelEntity().apply {
            id = "model-1"
            catalogId = "910244132"
            uri = "http://localhost:5050/model-1"
            data = mapOf("title" to mapOf("nb" to "Testmodell"), "modified" to value)
        }

        val actual = ModelFactory.createDefaultModel()
        writer.write(actual, entity)

        val literal = actual.listObjectsOfProperty(DCTerms.modified).toList().single().asLiteral()
        assertEquals(value, literal.lexicalForm)
        assertEquals(XSD.NS + xsdType, literal.datatypeURI)
    }

    @Test
    fun `invalid modified date is omitted`() {
        val entity = InformationModelEntity().apply {
            id = "model-1"
            catalogId = "910244132"
            uri = "http://localhost:5050/model-1"
            data = mapOf("title" to mapOf("nb" to "Testmodell"), "modified" to "15.03.2026")
        }

        val actual = ModelFactory.createDefaultModel()
        writer.write(actual, entity)

        assertFalse(actual.contains(null, DCTerms.modified))
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
