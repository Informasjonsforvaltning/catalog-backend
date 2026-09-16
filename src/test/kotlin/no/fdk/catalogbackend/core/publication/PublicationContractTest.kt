package no.fdk.catalogbackend.core.publication

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import no.fdk.catalogbackend.testsupport.PostgresTestcontainer
import no.fdk.catalogbackend.testsupport.fake.FakeResourceRepository
import no.fdk.catalogbackend.testsupport.jwt.Access
import no.fdk.catalogbackend.testsupport.jwt.CATALOG_ID
import no.fdk.catalogbackend.testsupport.jwt.JwtToken
import no.fdk.catalogbackend.testsupport.mockServer
import no.fdk.catalogbackend.testsupport.startMockServer
import no.fdk.catalogbackend.testsupport.stopMockServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainer::class)
class PublicationContractTest(@param:Autowired val mockMvc: MockMvc, @param:Autowired val fakeResourceRepository: FakeResourceRepository) {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() = startMockServer()

        @JvmStatic
        @AfterAll
        fun teardown() = stopMockServer()
    }

    @BeforeEach
    fun setUp() {
        fakeResourceRepository.deleteAll()
        mockServer().resetRequests()
        mockServer().stubFor(
            post(urlPathEqualTo("/organizations/$CATALOG_ID/datasources"))
                .willReturn(aResponse().withStatus(200)),
        )
        mockServer().stubFor(
            post(urlPathEqualTo("/organizations/$CATALOG_ID/datasources/start-harvesting"))
                .willReturn(aResponse().withStatus(200)),
        )
    }

    private fun bearer() = "Bearer ${JwtToken(Access.ORG_WRITE)}"

    private fun createResource(title: String = "Title"): String {
        val location = mockMvc
            .post("/catalogs/$CATALOG_ID/fake-resources") {
                header(HttpHeaders.AUTHORIZATION, bearer())
                contentType = MediaType.APPLICATION_JSON
                content = """{"title":"$title"}"""
            }.andExpect { status { isCreated() } }
            .andReturn()
            .response
            .getHeader(HttpHeaders.LOCATION)!!
        return location.substringAfterLast("/")
    }

    @Test
    fun `first publish registers a data source and triggers harvest`() {
        val id = createResource()

        mockMvc
            .post("/catalogs/$CATALOG_ID/fake-resources/$id/publish") {
                header(HttpHeaders.AUTHORIZATION, bearer())
            }.andExpect {
                status { isOk() }
                jsonPath("$.published") { value(true) }
                jsonPath("$.publishedDate") { exists() }
            }

        mockServer().verify(
            postRequestedFor(urlEqualTo("/organizations/$CATALOG_ID/datasources"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing("application/json"))
                .withHeader(HttpHeaders.AUTHORIZATION, containing("Bearer "))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                          "dataSourceType": "FAKE-AP-NO",
                          "dataType": "fake",
                          "url": "http://localhost:5050/graphs/catalogs/$CATALOG_ID/fake-resources",
                          "acceptHeaderValue": "text/turtle",
                          "publisherId": "$CATALOG_ID",
                          "description": "Automatically generated data source for $CATALOG_ID"
                        }
                        """.trimIndent(),
                    ),
                ),
        )
        mockServer().verify(
            postRequestedFor(urlEqualTo("/organizations/$CATALOG_ID/datasources/start-harvesting"))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                          "url": "http://localhost:5050/graphs/catalogs/$CATALOG_ID/fake-resources",
                          "dataType": "fake"
                        }
                        """.trimIndent(),
                    ),
                ),
        )
    }

    @Test
    fun `second publish in the same catalog only triggers harvest`() {
        val first = createResource("One")
        val second = createResource("Two")

        mockMvc
            .post("/catalogs/$CATALOG_ID/fake-resources/$first/publish") {
                header(HttpHeaders.AUTHORIZATION, bearer())
            }.andExpect { status { isOk() } }

        mockServer().resetRequests()

        mockMvc
            .post("/catalogs/$CATALOG_ID/fake-resources/$second/publish") {
                header(HttpHeaders.AUTHORIZATION, bearer())
            }.andExpect { status { isOk() } }

        mockServer().verify(0, postRequestedFor(urlEqualTo("/organizations/$CATALOG_ID/datasources")))
        mockServer().verify(
            1,
            postRequestedFor(urlEqualTo("/organizations/$CATALOG_ID/datasources/start-harvesting")),
        )
    }

    @Test
    fun `publishing an already published resource is a bad request`() {
        val id = createResource()
        mockMvc
            .post("/catalogs/$CATALOG_ID/fake-resources/$id/publish") {
                header(HttpHeaders.AUTHORIZATION, bearer())
            }.andExpect { status { isOk() } }

        mockMvc
            .post("/catalogs/$CATALOG_ID/fake-resources/$id/publish") {
                header(HttpHeaders.AUTHORIZATION, bearer())
            }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `unpublish triggers harvest and rejects a second unpublish`() {
        val id = createResource()
        mockMvc
            .post("/catalogs/$CATALOG_ID/fake-resources/$id/publish") {
                header(HttpHeaders.AUTHORIZATION, bearer())
            }.andExpect { status { isOk() } }

        mockServer().resetRequests()

        mockMvc
            .post("/catalogs/$CATALOG_ID/fake-resources/$id/unpublish") {
                header(HttpHeaders.AUTHORIZATION, bearer())
            }.andExpect {
                status { isOk() }
                jsonPath("$.published") { value(false) }
            }

        mockServer().verify(
            1,
            postRequestedFor(urlEqualTo("/organizations/$CATALOG_ID/datasources/start-harvesting")),
        )

        mockMvc
            .post("/catalogs/$CATALOG_ID/fake-resources/$id/unpublish") {
                header(HttpHeaders.AUTHORIZATION, bearer())
            }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `patching a published resource re-triggers harvest`() {
        val id = createResource()
        mockMvc
            .post("/catalogs/$CATALOG_ID/fake-resources/$id/publish") {
                header(HttpHeaders.AUTHORIZATION, bearer())
            }.andExpect { status { isOk() } }

        mockServer().resetRequests()

        mockMvc
            .patch("/catalogs/$CATALOG_ID/fake-resources/$id") {
                header(HttpHeaders.AUTHORIZATION, bearer())
                contentType = MediaType.APPLICATION_JSON
                content = """[{"op":"replace","path":"/title","value":"Updated"}]"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.title") { value("Updated") }
            }

        mockServer().verify(
            1,
            postRequestedFor(urlEqualTo("/organizations/$CATALOG_ID/datasources/start-harvesting")),
        )
    }

    @Test
    fun `patching an unpublished resource does not trigger harvest`() {
        val id = createResource()
        mockServer().resetRequests()

        mockMvc
            .patch("/catalogs/$CATALOG_ID/fake-resources/$id") {
                header(HttpHeaders.AUTHORIZATION, bearer())
                contentType = MediaType.APPLICATION_JSON
                content = """[{"op":"replace","path":"/title","value":"Draft"}]"""
            }.andExpect { status { isOk() } }

        mockServer().verify(
            0,
            postRequestedFor(urlPathEqualTo("/organizations/$CATALOG_ID/datasources/start-harvesting")),
        )
    }
}
