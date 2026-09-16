package no.fdk.catalogbackend.core.publication

import no.fdk.catalogbackend.core.model.ResourceType

data class HarvestCatalogEvent(val resourceType: ResourceType, val catalogId: String, val createDataSource: Boolean = false)
