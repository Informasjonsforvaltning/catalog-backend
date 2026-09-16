package no.fdk.catalogbackend.testsupport.fake

import jakarta.validation.Valid
import no.fdk.catalogbackend.core.model.JsonPatchOperation
import no.fdk.catalogbackend.core.web.CatalogResourceOperations
import no.fdk.catalogbackend.security.Authorities
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Test-only controller so publication behaviour can be exercised through the fake resource type.
 */
@RestController
@RequestMapping("/catalogs/{catalogId}/fake-resources")
class FakeResourceController(private val operations: CatalogResourceOperations, private val mapper: FakeResourceMapper) {
    @PreAuthorize(Authorities.READ)
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAll(@PathVariable catalogId: String): ResponseEntity<List<FakeDto>> =
        ResponseEntity.ok(operations.findAll(FAKE_RESOURCE, catalogId, mapper))

    @PreAuthorize(Authorities.WRITE)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun register(@PathVariable catalogId: String, @Valid @RequestBody values: FakeValues): ResponseEntity<Void> =
        operations.register(FAKE_RESOURCE, catalogId, "fake-resources", values, mapper)

    @PreAuthorize(Authorities.WRITE)
    @PatchMapping("/{id}", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun patch(
        @PathVariable catalogId: String,
        @PathVariable id: String,
        @Valid @RequestBody operations: List<JsonPatchOperation>,
    ): ResponseEntity<FakeDto> = ResponseEntity.ok(
        this.operations.patch(FAKE_RESOURCE, catalogId, id, operations, mapper, FakeDto::class.java),
    )

    @PreAuthorize(Authorities.WRITE)
    @DeleteMapping("/{id}")
    fun delete(@PathVariable catalogId: String, @PathVariable id: String): ResponseEntity<Void> {
        operations.delete(FAKE_RESOURCE, catalogId, id)
        return ResponseEntity.noContent().build()
    }

    @PreAuthorize(Authorities.WRITE)
    @PostMapping("/{id}/publish", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun publish(@PathVariable catalogId: String, @PathVariable id: String): ResponseEntity<FakeDto> =
        ResponseEntity.ok(operations.publish(FAKE_RESOURCE, catalogId, id, mapper))

    @PreAuthorize(Authorities.WRITE)
    @PostMapping("/{id}/unpublish", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun unpublish(@PathVariable catalogId: String, @PathVariable id: String): ResponseEntity<FakeDto> =
        ResponseEntity.ok(operations.unpublish(FAKE_RESOURCE, catalogId, id, mapper))
}
