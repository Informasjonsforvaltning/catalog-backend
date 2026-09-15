package no.fdk.catalogbackend.core.service

import no.fdk.catalogbackend.config.ApplicationProperties
import no.fdk.catalogbackend.core.model.ResourceType
import no.fdk.catalogbackend.core.spi.ResourceTypeMetadata
import org.springframework.stereotype.Service

/**
 * Mints public identifier URIs for catalogs and resources.
 */
@Service
class ResourceUriService(private val applicationProperties: ApplicationProperties, metadata: List<ResourceTypeMetadata>) {
    private val metadataByType = metadata.associateBy { it.resourceType }

    /**
     * Type-specific catalog URI: `{catalogIdentifierHost}/catalogs/{catalogId}/{pathSegment}`.
     */
    fun catalogUri(resourceType: ResourceType, catalogId: String): String {
        val pathSegment = metadata(resourceType).pathSegment
        return "${applicationProperties.catalogIdentifierHost}/$catalogId/$pathSegment"
    }

    /** Resource URI: `{identifierHost}/{pathSegment}/{id}`. */
    fun resourceUri(resourceType: ResourceType, id: String): String {
        val metadata = metadata(resourceType)
        return "${metadata.identifierHost}/$id"
    }

    private fun metadata(resourceType: ResourceType): ResourceTypeMetadata = metadataByType[resourceType]
        ?: throw IllegalArgumentException("No metadata registered for resource type $resourceType")
}
