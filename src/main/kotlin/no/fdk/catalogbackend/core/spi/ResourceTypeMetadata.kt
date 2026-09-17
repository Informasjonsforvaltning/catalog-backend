package no.fdk.catalogbackend.core.spi

import no.fdk.catalogbackend.core.model.ResourceType

interface ResourceTypeMetadata {
    val resourceType: ResourceType

    /** URL segment for this type, e.g. `information-models`. */
    val pathSegment: String

    /** Type-specific host used when minting public resource URIs. */
    val identifierHost: String

    /** harvest-admin data source type, e.g. `ModellDCAT-AP-NO`. */
    val dataSourceType: String

    /** harvest-admin data type, e.g. `informationmodel`. */
    val harvestDataType: String
}
