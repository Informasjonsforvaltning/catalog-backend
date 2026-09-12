package no.fdk.catalogbackend.core.model

data class CatalogCount(val catalogId: String, val counts: Map<String, Long>)
