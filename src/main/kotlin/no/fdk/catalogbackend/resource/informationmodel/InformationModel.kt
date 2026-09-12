package no.fdk.catalogbackend.resource.informationmodel

import no.fdk.catalogbackend.core.model.ContactPoint
import no.fdk.catalogbackend.core.model.LocalizedStrings
import java.time.Instant

data class InformationModelValues(
    val title: LocalizedStrings? = null,
    val description: LocalizedStrings? = null,
    val contactPoints: List<ContactPoint>? = null,
)

data class InformationModelDto(
    val id: String,
    val catalogId: String,
    val published: Boolean,
    val publishedDate: Instant? = null,
    val created: Instant,
    val lastModified: Instant? = null,
    val uri: String? = null,
    val title: LocalizedStrings? = null,
    val description: LocalizedStrings? = null,
    val contactPoints: List<ContactPoint>? = null,
)
