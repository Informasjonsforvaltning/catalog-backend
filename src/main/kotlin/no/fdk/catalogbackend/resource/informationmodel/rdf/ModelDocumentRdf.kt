package no.fdk.catalogbackend.resource.informationmodel.rdf

import no.fdk.catalogbackend.core.rdf.safeAddLinkedProperty
import no.fdk.catalogbackend.core.rdf.safeAddLocalizedString
import no.fdk.catalogbackend.core.rdf.safeCreateResource
import no.fdk.catalogbackend.resource.informationmodel.ModelDocument
import org.apache.jena.rdf.model.Resource
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS

fun Resource.addModelDocuments(documents: List<ModelDocument>?): Resource {
    documents?.forEach {
        val document = model
            .safeCreateResource()
            .addProperty(RDF.type, FOAF.Document)
            .safeAddLocalizedString(DCTerms.title, it.title)
            .safeAddLinkedProperty(DCTerms.language, it.language)
            .safeAddLinkedProperty(DCTerms.format, it.format)
            .safeAddLinkedProperty(RDFS.seeAlso, it.seeAlso)
        addProperty(DCTerms.hasFormat, document)
    }
    return this
}
