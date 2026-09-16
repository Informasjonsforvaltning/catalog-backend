package no.fdk.catalogbackend.core.publication

import no.fdk.catalogbackend.config.ApplicationProperties
import no.fdk.catalogbackend.core.spi.ResourceTypeMetadata
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(MockitoExtension::class)
@Tag("unit")
class HarvestAdminClientTest {
    @Mock
    private lateinit var restTemplate: RestTemplate

    private lateinit var client: HarvestAdminClient

    private val mapper = jacksonObjectMapper()
    private val catalogId = "910244132"
    private val catalogUrl = "http://localhost:5050/graphs/catalogs/$catalogId/fake-resources"
    private val metadata = mock<ResourceTypeMetadata> {
        on { dataSourceType } doReturn "FAKE-AP-NO"
        on { harvestDataType } doReturn "fake"
    }

    @BeforeEach
    fun setUp() {
        client = HarvestAdminClient(
            ApplicationProperties(
                catalogIdentifierHost = "http://localhost:5050",
                informationModelIdentifierHost = "http://localhost:5050",
                catalogBackendUri = "http://localhost:5050",
                organizationCatalogUri = "http://localhost:5050",
                harvestAdminUri = "http://harvest-admin:8080",
            ),
            restTemplate,
        )
    }

    @AfterEach
    fun cleanup() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `createNewDataSource posts to the correct url with the expected body`() {
        stubPost()

        client.createNewDataSource(catalogId, metadata, catalogUrl)

        val uriCaptor = ArgumentCaptor.forClass(URI::class.java)
        val entityCaptor = ArgumentCaptor.forClass(HttpEntity::class.java)
        verify(restTemplate).postForEntity(uriCaptor.capture(), entityCaptor.capture(), eq(Any::class.java))

        assertEquals(URI("http://harvest-admin:8080/organizations/$catalogId/datasources"), uriCaptor.value)

        val body = mapper.convertValue(entityCaptor.value.body!!, Map::class.java)
        assertEquals("FAKE-AP-NO", body["dataSourceType"])
        assertEquals("fake", body["dataType"])
        assertEquals(catalogUrl, body["url"])
        assertEquals("text/turtle", body["acceptHeaderValue"])
        assertEquals(catalogId, body["publisherId"])
        assertEquals("Automatically generated data source for $catalogId", body["description"])
        assertEquals(MediaType.APPLICATION_JSON, entityCaptor.value.headers.contentType)
    }

    @Test
    fun `createNewDataSource forwards the bearer token when authenticated`() {
        stubPost()
        setJwt("token-value")

        client.createNewDataSource(catalogId, metadata, catalogUrl)

        val entityCaptor = ArgumentCaptor.forClass(HttpEntity::class.java)
        verify(restTemplate).postForEntity(any<URI>(), entityCaptor.capture(), eq(Any::class.java))
        assertEquals("Bearer token-value", entityCaptor.value.headers.getFirst(HttpHeaders.AUTHORIZATION))
    }

    @Test
    fun `createNewDataSource omits authorization when unauthenticated`() {
        stubPost()

        client.createNewDataSource(catalogId, metadata, catalogUrl)

        val entityCaptor = ArgumentCaptor.forClass(HttpEntity::class.java)
        verify(restTemplate).postForEntity(any<URI>(), entityCaptor.capture(), eq(Any::class.java))
        assertNull(entityCaptor.value.headers.getFirst(HttpHeaders.AUTHORIZATION))
    }

    @Test
    fun `createNewDataSource does not throw when RestTemplate fails`() {
        whenever(restTemplate.postForEntity(any<URI>(), any<HttpEntity<*>>(), eq(Any::class.java)))
            .thenThrow(RestClientException("boom"))

        client.createNewDataSource(catalogId, metadata, catalogUrl)
    }

    @Test
    fun `triggerHarvest posts to the correct url with the expected body`() {
        stubPost()

        client.triggerHarvest(catalogId, metadata, catalogUrl)

        val uriCaptor = ArgumentCaptor.forClass(URI::class.java)
        val entityCaptor = ArgumentCaptor.forClass(HttpEntity::class.java)
        verify(restTemplate).postForEntity(uriCaptor.capture(), entityCaptor.capture(), eq(Any::class.java))

        assertEquals(
            URI("http://harvest-admin:8080/organizations/$catalogId/datasources/start-harvesting"),
            uriCaptor.value,
        )
        val body = mapper.convertValue(entityCaptor.value.body!!, Map::class.java)
        assertEquals(catalogUrl, body["url"])
        assertEquals("fake", body["dataType"])
    }

    @Test
    fun `triggerHarvest does not throw when RestTemplate fails`() {
        whenever(restTemplate.postForEntity(any<URI>(), any<HttpEntity<*>>(), eq(Any::class.java)))
            .thenThrow(RestClientException("boom"))

        client.triggerHarvest(catalogId, metadata, catalogUrl)
    }

    private fun stubPost() {
        whenever(restTemplate.postForEntity(any<URI>(), any<HttpEntity<*>>(), eq(Any::class.java)))
            .thenReturn(ResponseEntity.ok().build())
    }

    private fun setJwt(token: String) {
        val jwt = Jwt.withTokenValue(token)
            .header("alg", "none")
            .claim("sub", "user")
            .build()
        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(jwt)
    }
}
