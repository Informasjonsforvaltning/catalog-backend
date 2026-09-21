package no.fdk.catalogbackend.core.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Per-catalog resource counts, keyed by ResourceType key")
data class CatalogCount(val catalogId: String, val counts: Map<String, Long>)
