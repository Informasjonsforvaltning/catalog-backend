package no.fdk.catalogbackend.security

import no.fdk.catalogbackend.testsupport.PostgresTestcontainer
import no.fdk.catalogbackend.testsupport.jwt.Access
import no.fdk.catalogbackend.testsupport.jwt.CATALOG_ID
import no.fdk.catalogbackend.testsupport.jwt.JwtToken
import no.fdk.catalogbackend.testsupport.startMockServer
import no.fdk.catalogbackend.testsupport.stopMockServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional

@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainer::class)
@Transactional
class AuthoritiesTest(@param:Autowired val mockMvc: MockMvc) {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() = startMockServer()

        @JvmStatic
        @AfterAll
        fun teardown() = stopMockServer()
    }

    private fun bearer(access: Access) = "Bearer ${JwtToken(access)}"

    @ParameterizedTest
    @EnumSource(value = Access::class, names = ["ORG_READ", "ORG_WRITE", "ORG_ADMIN", "ROOT", "MULTIPLE_ORGS"])
    fun `read is granted to every role in the catalog and to root admin`(access: Access) {
        mockMvc
            .get("/internal/catalogs/$CATALOG_ID/information-models") { header(HttpHeaders.AUTHORIZATION, bearer(access)) }
            .andExpect { status { isOk() } }
    }

    @Test
    fun `read is denied to a user with access only to another catalog`() {
        mockMvc
            .get("/internal/catalogs/$CATALOG_ID/information-models") {
                header(HttpHeaders.AUTHORIZATION, bearer(Access.WRONG_ORG_WRITE))
            }.andExpect { status { isForbidden() } }
    }

    @ParameterizedTest
    @EnumSource(value = Access::class, names = ["ORG_WRITE", "ORG_ADMIN"])
    fun `write is granted to write and admin`(access: Access) {
        mockMvc
            .post("/internal/catalogs/$CATALOG_ID/information-models") {
                header(HttpHeaders.AUTHORIZATION, bearer(access))
                contentType = MediaType.APPLICATION_JSON
                content = """{"title":{"nb":"Modell"}}"""
            }.andExpect { status { isCreated() } }
    }

    @ParameterizedTest
    @EnumSource(value = Access::class, names = ["ORG_READ", "ROOT", "WRONG_ORG_WRITE"])
    fun `write is denied to read-only, root admin and other catalogs`(access: Access) {
        mockMvc
            .post("/internal/catalogs/$CATALOG_ID/information-models") {
                header(HttpHeaders.AUTHORIZATION, bearer(access))
                contentType = MediaType.APPLICATION_JSON
                content = """{"title":{"nb":"Modell"}}"""
            }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `delete is granted to write`() {
        val location = mockMvc
            .post("/internal/catalogs/$CATALOG_ID/information-models") {
                header(HttpHeaders.AUTHORIZATION, bearer(Access.ORG_WRITE))
                contentType = MediaType.APPLICATION_JSON
                content = """{"title":{"nb":"Modell"}}"""
            }.andExpect { status { isCreated() } }
            .andReturn()
            .response
            .getHeader(HttpHeaders.LOCATION)!!

        mockMvc
            .delete(location) { header(HttpHeaders.AUTHORIZATION, bearer(Access.ORG_WRITE)) }
            .andExpect { status { isNoContent() } }
    }
}
