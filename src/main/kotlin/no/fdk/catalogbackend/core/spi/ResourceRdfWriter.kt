package no.fdk.catalogbackend.core.spi

import no.fdk.catalogbackend.core.model.ResourceType
import no.fdk.catalogbackend.core.persistence.CatalogResourceEntity
import org.apache.jena.rdf.model.Model

interface ResourceRdfWriter {
    val resourceType: ResourceType

    /** Writes the catalog shell and its membership links for [members]. */
    fun writeCatalog(model: Model, catalogId: String, members: List<CatalogResourceEntity>)

    /** Writes a single resource, does not emit catalog triples. */
    fun write(model: Model, entity: CatalogResourceEntity)
}
