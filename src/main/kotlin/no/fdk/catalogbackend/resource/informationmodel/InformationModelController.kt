package no.fdk.catalogbackend.resource.informationmodel

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "Information models")
@RestController
@RequestMapping("/catalogs/{catalogId}/information-models")
class InformationModelController(private val operations: CatalogResourceOperations, private val mapper: InformationModelMapper) {
    @Operation(summary = "List information models in a catalog")
    @PreAuthorize(Authorities.READ)
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAll(@PathVariable catalogId: String): ResponseEntity<List<InformationModelDto>> =
        ResponseEntity.ok(operations.findAll(INFORMATION_MODEL, catalogId, mapper))

    @Operation(summary = "Get an information model by id")
    @PreAuthorize(Authorities.READ)
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findById(@PathVariable catalogId: String, @PathVariable id: String): ResponseEntity<InformationModelDto> =
        ResponseEntity.ok(operations.findById(INFORMATION_MODEL, catalogId, id, mapper))

    @Operation(summary = "Register a new information model")
    @PreAuthorize(Authorities.WRITE)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun register(@PathVariable catalogId: String, @Valid @RequestBody values: InformationModelValues): ResponseEntity<Void> =
        operations.register(INFORMATION_MODEL, catalogId, "information-models", values, mapper)

    @Operation(summary = "Patch an information model (RFC 6902)")
    @PreAuthorize(Authorities.WRITE)
    @PatchMapping("/{id}", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun patch(
        @PathVariable catalogId: String,
        @PathVariable id: String,
        @Valid @RequestBody operations: List<JsonPatchOperation>,
    ): ResponseEntity<InformationModelDto> = ResponseEntity.ok(
        this.operations.patch(
            INFORMATION_MODEL,
            catalogId,
            id,
            operations,
            mapper,
            InformationModelDto::class.java,
        ),
    )

    @Operation(summary = "Delete an information model")
    @PreAuthorize(Authorities.WRITE)
    @DeleteMapping("/{id}")
    fun delete(@PathVariable catalogId: String, @PathVariable id: String): ResponseEntity<Void> {
        operations.delete(INFORMATION_MODEL, catalogId, id)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Publish an information model and trigger harvest")
    @PreAuthorize(Authorities.WRITE)
    @PostMapping("/{id}/publish", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun publish(@PathVariable catalogId: String, @PathVariable id: String): ResponseEntity<InformationModelDto> =
        ResponseEntity.ok(operations.publish(INFORMATION_MODEL, catalogId, id, mapper))

    @Operation(summary = "Unpublish an information model and trigger harvest")
    @PreAuthorize(Authorities.WRITE)
    @PostMapping("/{id}/unpublish", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun unpublish(@PathVariable catalogId: String, @PathVariable id: String): ResponseEntity<InformationModelDto> =
        ResponseEntity.ok(operations.unpublish(INFORMATION_MODEL, catalogId, id, mapper))
}
