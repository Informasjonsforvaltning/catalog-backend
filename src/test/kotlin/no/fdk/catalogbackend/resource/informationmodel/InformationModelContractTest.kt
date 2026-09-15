package no.fdk.catalogbackend.resource.informationmodel

import no.fdk.catalogbackend.testsupport.PostgresTestcontainer
import no.fdk.catalogbackend.testsupport.jwt.Access
import no.fdk.catalogbackend.testsupport.jwt.CATALOG_ID
import no.fdk.catalogbackend.testsupport.jwt.JwtToken
import no.fdk.catalogbackend.testsupport.jwt.OTHER_CATALOG_ID
import no.fdk.catalogbackend.testsupport.startMockServer
import no.fdk.catalogbackend.testsupport.stopMockServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainer::class)
@Transactional
class InformationModelContractTest(@param:Autowired val mockMvc: MockMvc) {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() = startMockServer()

        @JvmStatic
        @AfterAll
        fun teardown() = stopMockServer()
    }

    private fun bearer(access: Access = Access.ORG_WRITE) = "Bearer ${JwtToken(access)}"

    private fun createModel(body: String = minimalModelJson()): String = mockMvc
        .post("/catalogs/$CATALOG_ID/information-models") {
            header(HttpHeaders.AUTHORIZATION, bearer())
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isCreated() }
        }
        .andReturn()
        .response
        .getHeader(HttpHeaders.LOCATION)!!

    private fun minimalModelJson() =
        """
        {
          "title": {"nb": "Modell"},
          "description": {"nb": "Modellbeskrivelse"}
        }
        """.trimIndent()

    @Test
    fun `full crud lifecycle`() {
        val location = createModel()
        assertTrue(location.startsWith("/catalogs/$CATALOG_ID/information-models/"))

        mockMvc
            .get(location) { header(HttpHeaders.AUTHORIZATION, bearer(Access.ORG_READ)) }
            .andExpect {
                status { isOk() }
                jsonPath("$.title.nb") { value("Modell") }
                jsonPath("$.description.nb") { value("Modellbeskrivelse") }
                jsonPath("$.published") { value(false) }
                jsonPath("$.uri") { value(org.hamcrest.Matchers.containsString("/information-models/")) }
            }

        mockMvc
            .patch(location) {
                header(HttpHeaders.AUTHORIZATION, bearer())
                contentType = MediaType.APPLICATION_JSON
                content = """[{"op":"replace","path":"/title/nb","value":"Oppdatert"}]"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.title.nb") { value("Oppdatert") }
            }

        mockMvc
            .get("/catalogs/$CATALOG_ID/information-models") {
                header(HttpHeaders.AUTHORIZATION, bearer(Access.ORG_READ))
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(1) }
            }

        mockMvc
            .delete(location) { header(HttpHeaders.AUTHORIZATION, bearer()) }
            .andExpect { status { isNoContent() } }

        mockMvc
            .get(location) { header(HttpHeaders.AUTHORIZATION, bearer(Access.ORG_READ)) }
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `patching an immutable path is rejected`() {
        val location = createModel()

        mockMvc
            .patch(location) {
                header(HttpHeaders.AUTHORIZATION, bearer())
                contentType = MediaType.APPLICATION_JSON
                content = """[{"op":"replace","path":"/id","value":"hijacked"}]"""
            }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `count endpoint scopes non-root callers to their organizations`() {
        createModel()
        mockMvc
            .post("/catalogs/$OTHER_CATALOG_ID/information-models") {
                header(HttpHeaders.AUTHORIZATION, bearer(Access.WRONG_ORG_WRITE))
                contentType = MediaType.APPLICATION_JSON
                content = """{"title":{"nb":"Other"}}"""
            }.andExpect { status { isCreated() } }

        val orgScoped = mockMvc
            .get("/catalogs/count") { header(HttpHeaders.AUTHORIZATION, bearer(Access.ORG_READ)) }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString

        assertTrue(orgScoped.contains(CATALOG_ID))
        assertTrue(!orgScoped.contains(OTHER_CATALOG_ID))

        val root = mockMvc
            .get("/catalogs/count") { header(HttpHeaders.AUTHORIZATION, bearer(Access.ROOT)) }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString

        assertTrue(root.contains(CATALOG_ID))
        assertTrue(root.contains(OTHER_CATALOG_ID))
        assertEquals(true, root.contains("INFORMATION_MODEL") || root.contains("\"INFORMATION_MODEL\""))
    }
}
