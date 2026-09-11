package no.fdk.catalogbackend.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("application")
data class ApplicationProperties(val catalogBackendUri: String, val organizationCatalogUri: String, val harvestAdminUri: String)
