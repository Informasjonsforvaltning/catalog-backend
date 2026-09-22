package no.fdk.catalogbackend.core.rdf.vocabulary

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory

object ADMS {
    const val URI = "http://www.w3.org/ns/adms#"

    val status: Property = ResourceFactory.createProperty("${URI}status")
}
