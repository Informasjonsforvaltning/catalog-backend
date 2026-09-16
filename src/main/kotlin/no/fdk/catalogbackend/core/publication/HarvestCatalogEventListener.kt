package no.fdk.catalogbackend.core.publication

import no.fdk.catalogbackend.config.ApplicationProperties
import no.fdk.catalogbackend.core.spi.ResourceRegistry
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class HarvestCatalogEventListener(
    private val registry: ResourceRegistry,
    private val applicationProperties: ApplicationProperties,
    private val harvestAdminClient: HarvestAdminClient,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onHarvestCatalog(event: HarvestCatalogEvent) {
        val metadata = registry.metadata(event.resourceType)
        val catalogUrl =
            "${applicationProperties.catalogBackendUri}/graphs/catalogs/${event.catalogId}/${metadata.pathSegment}"

        if (event.createDataSource) {
            harvestAdminClient.createNewDataSource(event.catalogId, metadata, catalogUrl)
        }
        harvestAdminClient.triggerHarvest(event.catalogId, metadata, catalogUrl)
    }
}
