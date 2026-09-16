package no.fdk.catalogbackend.core.publication

import no.fdk.catalogbackend.config.ApplicationProperties
import no.fdk.catalogbackend.core.spi.ResourceTypeMetadata
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import java.net.URI

@Component
class HarvestAdminClient(private val applicationProperties: ApplicationProperties, private val restTemplate: RestTemplate) {
    fun createNewDataSource(catalogId: String, metadata: ResourceTypeMetadata, catalogUrl: String) {
        val url = "${applicationProperties.harvestAdminUri}/organizations/$catalogId/datasources"
        val body = HarvestAdminDataSource(
            dataSourceType = metadata.dataSourceType,
            dataType = metadata.harvestDataType,
            url = catalogUrl,
            acceptHeaderValue = "text/turtle",
            publisherId = catalogId,
            description = "Automatically generated data source for $catalogId",
        )

        post(url, body, "createDataSource", catalogId)
    }

    fun triggerHarvest(catalogId: String, metadata: ResourceTypeMetadata, catalogUrl: String) {
        val url = "${applicationProperties.harvestAdminUri}/organizations/$catalogId/datasources/start-harvesting"
        val body = StartHarvestByUrlRequest(url = catalogUrl, dataType = metadata.harvestDataType)

        post(url, body, "startHarvestingByUrlAndDataType", catalogId)
    }

    private fun post(url: String, body: Any, operation: String, catalogId: String) {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            resolveBearerToken()?.let { set(HttpHeaders.AUTHORIZATION, "Bearer $it") }
        }

        runCatching {
            restTemplate.postForEntity<Any>(URI(url), HttpEntity(body, headers))
        }.onFailure {
            logger.error("Error calling Harvest Admin $operation for catalog {}", catalogId, it)
        }
    }

    private fun resolveBearerToken(): String? = (SecurityContextHolder.getContext().authentication?.principal as? Jwt)?.tokenValue

    companion object {
        private val logger = LoggerFactory.getLogger(HarvestAdminClient::class.java)
    }
}
