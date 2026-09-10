package no.fdk.catalogbackend.integration.security

import no.fdk.catalogbackend.integration.config.WebMvcTestSecurityConfig
import no.fdk.catalogbackend.testsupport.TestResourceController
import no.fdk.catalogbackend.utils.jwt.CATALOG_ID
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@Tag("integration")
@ActiveProfiles("test")
@Import(WebMvcTestSecurityConfig::class)
@WebMvcTest(controllers = [TestResourceController::class])
class AuthoritiesSliceTest(@param:Autowired val mockMvc: MockMvc) {
    @ParameterizedTest
    @ValueSource(strings = ["system:root:admin", "organization:%s:admin", "organization:%s:write", "organization:%s:read"])
    fun `read is granted to every role in the catalog and to root admin`(authority: String) {
        mockMvc
            .get("/internal/test/$CATALOG_ID") {
                with(jwt().authorities(SimpleGrantedAuthority(authority.format(CATALOG_ID))))
            }.andExpect { status { isOk() } }
    }

    @Test
    fun `read is denied on an unrelated authority`() {
        mockMvc
            .get("/internal/test/$CATALOG_ID") {
                with(jwt().authorities(SimpleGrantedAuthority("invalid")))
            }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `unauthenticated requests are rejected`() {
        mockMvc.get("/internal/test/$CATALOG_ID")
            .andExpect { status { isUnauthorized() } }
    }
}
