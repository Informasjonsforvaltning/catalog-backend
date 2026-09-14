package no.fdk.catalogbackend.core.spi

import no.fdk.catalogbackend.core.model.ResourceType
import no.fdk.catalogbackend.core.persistence.CatalogResourceEntity
import org.apache.jena.rdf.model.Model

interface ResourceRdfWriter {
    val resourceType: ResourceType

    fun write(model: Model, entity: CatalogResourceEntity)
}
