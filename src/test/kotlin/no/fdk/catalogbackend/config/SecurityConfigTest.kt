package no.fdk.catalogbackend.config

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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainer::class)
class SecurityConfigTest(@param:Autowired val mockMvc: MockMvc) {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() = startMockServer()

        @JvmStatic
        @AfterAll
        fun teardown() = stopMockServer()
    }

    @Test
    fun `ping is open`() {
        mockMvc.get("/ping")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `ready is open`() {
        mockMvc.get("/ready")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `authenticated catalog endpoints reject requests without a token`() {
        mockMvc.get("/catalogs/$CATALOG_ID/information-models")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `authenticated catalog endpoints reject a malformed token`() {
        mockMvc
            .get("/catalogs/$CATALOG_ID/information-models") {
                header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
            }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `token issued for another audience is rejected`() {
        val token = JwtToken(Access.ORG_ADMIN, audience = "some-other-service")

        mockMvc
            .get("/catalogs/$CATALOG_ID/information-models") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `token issued by another issuer is rejected`() {
        val token = JwtToken(Access.ORG_ADMIN, issuer = "https://evil.example.com/realms/fdk")

        mockMvc
            .get("/catalogs/$CATALOG_ID/information-models") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `valid token authenticates and reaches the resource controller`() {
        val token = JwtToken(Access.ORG_ADMIN)

        mockMvc
            .get("/catalogs/$CATALOG_ID/information-models") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            }.andExpect {
                status { isOk() }
                content { string("[]") }
            }
    }

    @Test
    fun `rdf graph endpoints are open without a token`() {
        mockMvc
            .get("/graphs/catalogs/$CATALOG_ID/information-models") {
                header(HttpHeaders.ACCEPT, "text/turtle")
            }.andExpect { status { isOk() } }
    }
}
