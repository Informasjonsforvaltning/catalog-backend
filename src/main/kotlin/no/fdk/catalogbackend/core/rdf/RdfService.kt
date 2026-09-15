package no.fdk.catalogbackend.core.rdf

import no.fdk.catalogbackend.core.spi.ResourceRegistry
import no.fdk.catalogbackend.exception.NotFoundException
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.VCARD4
import org.springframework.stereotype.Service

@Service
class RdfService(private val registry: ResourceRegistry) {
    fun serializeAll(lang: Lang): String {
        val model = createModel()
        registry.stores().forEach { store ->
            val writer = registry.rdfWriter(store.resourceType)
            store.findAllPublished().forEach { writer.write(model, it) }
        }
        return model.createRDFResponse(lang)
    }

    fun serializeCatalog(catalogId: String, lang: Lang): String {
        val model = createModel()
        registry.stores().forEach { store ->
            val writer = registry.rdfWriter(store.resourceType)
            store.findAllPublished(catalogId).forEach { writer.write(model, it) }
        }
        return model.createRDFResponse(lang)
    }

    fun serializeResource(pathSegment: String, id: String, lang: Lang): String {
        val resourceType = registry.resourceTypes.firstOrNull { registry.metadata(it).pathSegment == pathSegment }
            ?: throw NotFoundException("Unknown resource path segment: $pathSegment")

        val entity = registry.store(resourceType).findPublishedById(id)
            ?: throw NotFoundException("No published $resourceType with id $id")

        val model = createModel()
        registry.rdfWriter(resourceType).write(model, entity)
        return model.createRDFResponse(lang)
    }

    private fun createModel(): Model = ModelFactory.createDefaultModel().apply {
        setNsPrefixes(
            mapOf(
                "dcat" to DCAT.NS,
                "dct" to DCTerms.NS,
                "rdf" to RDF.uri,
                "foaf" to FOAF.NS,
                "vcard" to VCARD4.NS,
            ),
        )
    }
}
