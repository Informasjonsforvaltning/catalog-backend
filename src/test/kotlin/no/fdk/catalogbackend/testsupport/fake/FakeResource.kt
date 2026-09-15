package no.fdk.catalogbackend.testsupport.fake

import jakarta.persistence.Entity
import jakarta.persistence.Table
import no.fdk.catalogbackend.core.model.ResourceType
import no.fdk.catalogbackend.core.persistence.CatalogResourceEntity
import no.fdk.catalogbackend.core.persistence.CatalogResourceRepository
import no.fdk.catalogbackend.core.persistence.JpaResourceStore
import no.fdk.catalogbackend.core.service.ResourceUriService
import no.fdk.catalogbackend.core.spi.ResourceMapper
import no.fdk.catalogbackend.core.spi.ResourceRdfWriter
import no.fdk.catalogbackend.core.spi.ResourceTypeMetadata
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF
import org.springframework.stereotype.Component

/**
 * A resource type that exists only in tests.
 */
val FAKE_RESOURCE = ResourceType("FAKE")

@Entity
@Table(name = "fake_resources")
class FakeResourceEntity : CatalogResourceEntity()

interface FakeResourceRepository : CatalogResourceRepository<FakeResourceEntity>

@Component
class FakeResourceStore(repository: FakeResourceRepository) :
    JpaResourceStore<FakeResourceEntity>(FAKE_RESOURCE, repository, FakeResourceEntity::class, ::FakeResourceEntity)

@Component
class FakeResourceMetadata : ResourceTypeMetadata {
    override val resourceType = FAKE_RESOURCE
    override val pathSegment = "fake-resources"
    override val identifierHost = "http://localhost:5050"
    override val dataSourceType = "FAKE-AP-NO"
    override val harvestDataType = "fake"
}

data class FakeValues(val title: String? = null, val description: String? = null)

data class FakeDto(val id: String, val catalogId: String, val published: Boolean, val title: String?, val description: String?)

@Component
class FakeResourceMapper : ResourceMapper<FakeValues, FakeDto> {
    override val resourceType = FAKE_RESOURCE

    override fun toPayload(values: FakeValues): Map<String, Any?> = mapOf("title" to values.title, "description" to values.description)

    override fun toDto(entity: CatalogResourceEntity): FakeDto = FakeDto(
        id = entity.id,
        catalogId = entity.catalogId,
        published = entity.published,
        title = entity.data?.get("title") as String?,
        description = entity.data?.get("description") as String?,
    )

    override fun toValues(dto: FakeDto): FakeValues = FakeValues(title = dto.title, description = dto.description)
}

@Component
class FakeResourceRdfWriter(private val resourceUriService: ResourceUriService) : ResourceRdfWriter {
    override val resourceType = FAKE_RESOURCE

    override fun write(model: Model, entity: CatalogResourceEntity) {
        val catalog = model.createResource(resourceUriService.catalogUri(FAKE_RESOURCE, entity.catalogId))
            .addProperty(RDF.type, DCAT.Catalog)
        val resourceUri = entity.uri ?: resourceUriService.resourceUri(FAKE_RESOURCE, entity.id)
        val resource = model.createResource(resourceUri)
            .addProperty(RDF.type, ResourceFactory.createResource("https://example.com/ns#FakeResource"))
        catalog.addProperty(ResourceFactory.createProperty("https://example.com/ns#fake"), resource)
    }
}
