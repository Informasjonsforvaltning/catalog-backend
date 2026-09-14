package no.fdk.catalogbackend.core.rdf.vocabulary

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory

/**
 * ModellDCAT-AP-NO vocabulary (copied from fdk-parser-service).
 */
object MODELLDCATNO {
    const val URI = "https://data.norge.no/vocabulary/modelldcatno#"

    val model: Property = ResourceFactory.createProperty("${URI}model")

    val InformationModel: Resource = ResourceFactory.createResource("${URI}InformationModel")
}
