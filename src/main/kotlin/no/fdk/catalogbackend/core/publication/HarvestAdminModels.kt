package no.fdk.catalogbackend.core.publication

data class HarvestAdminDataSource(
    val dataSourceType: String,
    val dataType: String,
    val url: String,
    val acceptHeaderValue: String = "text/turtle",
    val publisherId: String,
    val description: String? = null,
)

data class StartHarvestByUrlRequest(val url: String, val dataType: String)
