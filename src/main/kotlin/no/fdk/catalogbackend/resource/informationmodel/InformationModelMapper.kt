package no.fdk.catalogbackend.resource.informationmodel

import no.fdk.catalogbackend.core.persistence.CatalogResourceEntity
import no.fdk.catalogbackend.core.spi.ResourceMapper
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class InformationModelMapper(private val objectMapper: ObjectMapper) : ResourceMapper<InformationModelValues, InformationModelDto> {
    override val resourceType = INFORMATION_MODEL

    override fun toPayload(values: InformationModelValues): Map<String, Any?> =
        objectMapper.convertValue(values, Map::class.java).mapKeys { it.key.toString() }.mapValues { it.value }

    override fun toDto(entity: CatalogResourceEntity): InformationModelDto {
        val values = objectMapper.convertValue(entity.data ?: emptyMap<String, Any?>(), InformationModelValues::class.java)

        return InformationModelDto(
            id = entity.id,
            catalogId = entity.catalogId,
            published = entity.published,
            publishedDate = entity.publishedDate,
            created = entity.created,
            lastModified = entity.lastModified,
            uri = entity.uri,
            title = values.title,
            description = values.description,
            contactPoints = values.contactPoints,
        )
    }

    override fun toValues(dto: InformationModelDto): InformationModelValues = InformationModelValues(
        title = dto.title,
        description = dto.description,
        contactPoints = dto.contactPoints,
    )
}
