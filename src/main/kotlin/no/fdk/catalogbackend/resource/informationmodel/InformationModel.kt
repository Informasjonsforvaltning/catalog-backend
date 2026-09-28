package no.fdk.catalogbackend.resource.informationmodel

import io.swagger.v3.oas.annotations.media.Schema
import no.fdk.catalogbackend.core.model.ContactPoint
import no.fdk.catalogbackend.core.model.LocalizedStrings
import java.time.Instant

@Schema(description = "Writable information-model fields (jsonb payload)")
data class InformationModelValues(
    val title: LocalizedStrings? = null,
    val description: LocalizedStrings? = null,
    val contactPoints: List<ContactPoint>? = null,
    val status: String? = null,
    val homepage: String? = null,
)

@Schema(description = "Information model including server-owned metadata")
data class InformationModelDto(
    val id: String,
    val catalogId: String,
    val published: Boolean,
    @field:Schema(description = "Instant of the first successful publish, is retained across unpublish/republish")
    val publishedDate: Instant? = null,
    val created: Instant,
    val lastModified: Instant? = null,
    val uri: String? = null,
    val title: LocalizedStrings? = null,
    val description: LocalizedStrings? = null,
    val contactPoints: List<ContactPoint>? = null,
    @field:Schema(description = "Model maturity status (adms:status). URI from the EU Product status vocabulary.")
    val status: String? = null,
    @field:Schema(description = "Homepage for the information model (foaf:homepage). URI of a document with more information.")
    val homepage: String? = null,
)
