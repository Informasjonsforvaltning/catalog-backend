package no.fdk.catalogbackend.core.web

import no.fdk.catalogbackend.core.rdf.createRDFResponse
import no.fdk.catalogbackend.resource.informationmodel.InformationModelEntity
import no.fdk.catalogbackend.resource.informationmodel.InformationModelRepository
import no.fdk.catalogbackend.testsupport.PostgresTestcontainer
import no.fdk.catalogbackend.testsupport.checkIfIsomorphicAndPrintDiff
import no.fdk.catalogbackend.testsupport.jwt.Access
import no.fdk.catalogbackend.testsupport.jwt.CATALOG_ID
import no.fdk.catalogbackend.testsupport.jwt.JwtToken
import no.fdk.catalogbackend.testsupport.loadTurtle
import no.fdk.catalogbackend.testsupport.startMockServer
import no.fdk.catalogbackend.testsupport.stopMockServer
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import java.io.StringReader
import java.time.Instant
import kotlin.test.assertTrue

@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainer::class)
@Transactional
class RDFControllerTest(
    @param:Autowired val mockMvc: MockMvc,
    @param:Autowired val informationModelRepository: InformationModelRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() = startMockServer()

        @JvmStatic
        @AfterAll
        fun teardown() = stopMockServer()
    }

    private fun bearer() = "Bearer ${JwtToken(Access.ORG_WRITE)}"

    private fun persistPublished(id: String = "model-1", published: Boolean = true): InformationModelEntity =
        informationModelRepository.save(
            InformationModelEntity().apply {
                this.id = id
                catalogId = CATALOG_ID
                this.published = published
                created = Instant.now()
                lastModified = Instant.now()
                uri = "http://localhost:5050/$id"
                data = mapOf(
                    "title" to mapOf("nb" to "Testmodell"),
                    "description" to mapOf("nb" to "En beskrivelse"),
                    "status" to "http://publications.europa.eu/resource/authority/product-status/PRODUCTION",
                    "contactPoints" to listOf(
                        mapOf(
                            "name" to mapOf("nb" to "Kontakt"),
                            "email" to "kontakt@example.com",
                            "telephone" to "+47 12 34 56 78",
                        ),
                    ),
                )
            },
        )

    @Test
    fun `published information model is served as turtle isomorphic with the golden fixture`() {
        persistPublished()

        val body = mockMvc
            .get("/graphs/information-models/model-1") {
                header(HttpHeaders.ACCEPT, "text/turtle")
            }.andExpect {
                status { isOk() }
                content { contentTypeCompatibleWith("text/turtle") }
            }.andReturn()
            .response
            .contentAsString

        val actual = ModelFactory.createDefaultModel().read(StringReader(body), null, "TURTLE")
        assertTrue(checkIfIsomorphicAndPrintDiff(actual, loadTurtle("rdf/information_model.ttl"), "rdf-endpoint", logger))
    }

    @Test
    fun `unpublished resources are not exposed on the public rdf endpoints`() {
        val location = mockMvc
            .post("/catalogs/$CATALOG_ID/information-models") {
                header(HttpHeaders.AUTHORIZATION, bearer())
                contentType = MediaType.APPLICATION_JSON
                content = """{"title": {"nb": "Utkast"}}"""
            }.andExpect { status { isCreated() } }
            .andReturn()
            .response
            .getHeader(HttpHeaders.LOCATION)!!
        val id = location.substringAfterLast("/")

        mockMvc
            .get("/graphs/information-models/$id") {
                header(HttpHeaders.ACCEPT, "text/turtle")
            }.andExpect { status { isNotFound() } }

        val catalogBody = mockMvc
            .get("/graphs/catalogs/$CATALOG_ID/information-models") {
                header(HttpHeaders.ACCEPT, "text/turtle")
            }.andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString

        val catalogModel = ModelFactory.createDefaultModel().read(StringReader(catalogBody), null, "TURTLE")
        assertTrue(catalogModel.isEmpty)
    }

    @Test
    fun `unsupported accept header yields 406`() {
        persistPublished()

        mockMvc
            .get("/graphs/catalogs/$CATALOG_ID/information-models") {
                header(HttpHeaders.ACCEPT, "application/json")
            }.andExpect { status { isNotAcceptable() } }
    }

    @Test
    fun `json-ld is negotiated`() {
        persistPublished(id = "model-jsonld")

        val body = mockMvc
            .get("/graphs/information-models/model-jsonld") {
                header(HttpHeaders.ACCEPT, "application/ld+json")
            }.andExpect {
                status { isOk() }
                content { contentTypeCompatibleWith("application/ld+json") }
            }.andReturn()
            .response
            .contentAsString

        val actual = ModelFactory.createDefaultModel().read(StringReader(body), null, Lang.JSONLD.name)
        assertTrue(actual.size() > 0)
        assertTrue(actual.createRDFResponse(Lang.TURTLE).isNotBlank())
    }
}
