package no.fdk.catalogbackend.core.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.fdk.catalogbackend.core.model.CatalogCount
import no.fdk.catalogbackend.core.service.CatalogResourceService
import no.fdk.catalogbackend.security.Authorities
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Catalog counts")
@RestController
@RequestMapping("/catalogs/count")
class CountController(private val service: CatalogResourceService) {
    @Operation(
        summary = "Resource counts per catalog",
        description = "Callers are scoped to their organizations.",
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun counts(authentication: Authentication): ResponseEntity<List<CatalogCount>> {
        val authorities = authentication.authorities.mapNotNull { it.authority }
        val isRoot = Authorities.ROOT_ADMIN in authorities

        val perType = if (isRoot) {
            service.countsPerCatalog(null)
        } else {
            service.countsPerCatalog(organizationIds(authorities))
        }

        val catalogIds = perType.values.flatMap { it.keys }.toSortedSet()
        val response = catalogIds.map { catalogId ->
            CatalogCount(
                catalogId = catalogId,
                counts = perType.mapKeys { it.key.key }.mapValues { (_, byCatalog) -> byCatalog[catalogId] ?: 0L },
            )
        }

        return ResponseEntity.ok(response)
    }

    private fun organizationIds(authorities: List<String>): Set<String> = authorities.mapNotNull { authority ->
        val parts = authority.split(':')
        if (parts.size == 3 && parts[0] == "organization") parts[1] else null
    }.toSet()
}
