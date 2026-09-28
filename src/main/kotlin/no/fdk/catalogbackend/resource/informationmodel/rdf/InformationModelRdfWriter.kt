package no.fdk.catalogbackend.resource.informationmodel.rdf

import no.fdk.catalogbackend.config.ApplicationProperties
import no.fdk.catalogbackend.core.persistence.CatalogResourceEntity
import no.fdk.catalogbackend.core.rdf.addContactPoints
import no.fdk.catalogbackend.core.rdf.safeAddLinkedProperty
import no.fdk.catalogbackend.core.rdf.safeAddLocalizedString
import no.fdk.catalogbackend.core.rdf.safeCreateResource
import no.fdk.catalogbackend.core.rdf.vocabulary.ADMS
import no.fdk.catalogbackend.core.rdf.vocabulary.MODELLDCATNO
import no.fdk.catalogbackend.core.service.ResourceUriService
import no.fdk.catalogbackend.core.spi.ResourceRdfWriter
import no.fdk.catalogbackend.resource.informationmodel.INFORMATION_MODEL
import no.fdk.catalogbackend.resource.informationmodel.InformationModelValues
import org.apache.jena.rdf.model.Model
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class InformationModelRdfWriter(
    private val resourceUriService: ResourceUriService,
    private val applicationProperties: ApplicationProperties,
    private val objectMapper: ObjectMapper,
) : ResourceRdfWriter {
    override val resourceType = INFORMATION_MODEL

    override fun write(model: Model, entity: CatalogResourceEntity) {
        model.setNsPrefix("modelldcatno", MODELLDCATNO.URI)
        model.setNsPrefix("adms", ADMS.URI)

        val values = objectMapper.convertValue(entity.data ?: emptyMap<String, Any?>(), InformationModelValues::class.java)
        val catalogUri = resourceUriService.catalogUri(INFORMATION_MODEL, entity.catalogId)
        val organizationUri = "${applicationProperties.organizationCatalogUri}/organizations/${entity.catalogId}"
        val resourceUri = entity.uri ?: resourceUriService.resourceUri(INFORMATION_MODEL, entity.id)

        model.safeCreateResource(organizationUri)
            .addProperty(RDF.type, FOAF.Agent)
            .addProperty(DCTerms.identifier, entity.catalogId)

        val catalog = model.safeCreateResource(catalogUri)
            .addProperty(RDF.type, DCAT.Catalog)
            .addProperty(DCTerms.publisher, model.safeCreateResource(organizationUri))

        val informationModel = model.safeCreateResource(resourceUri)
            .addProperty(RDF.type, MODELLDCATNO.InformationModel)
            .addProperty(DCTerms.publisher, model.safeCreateResource(organizationUri))
            .safeAddLocalizedString(DCTerms.title, values.title)
            .safeAddLocalizedString(DCTerms.description, values.description)
            .safeAddLinkedProperty(ADMS.status, values.status)
            .safeAddLinkedProperty(FOAF.homepage, values.homepage)
            .addContactPoints(values.contactPoints)

        catalog.addProperty(MODELLDCATNO.model, informationModel)
    }
}
