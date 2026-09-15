package no.fdk.catalogbackend.resource.informationmodel

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

@RestController
@RequestMapping("/catalogs/{catalogId}/information-models")
class InformationModelController(private val operations: CatalogResourceOperations, private val mapper: InformationModelMapper) {
    @PreAuthorize(Authorities.READ)
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAll(@PathVariable catalogId: String): ResponseEntity<List<InformationModelDto>> =
        ResponseEntity.ok(operations.findAll(INFORMATION_MODEL, catalogId, mapper))

    @PreAuthorize(Authorities.READ)
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findById(@PathVariable catalogId: String, @PathVariable id: String): ResponseEntity<InformationModelDto> =
        ResponseEntity.ok(operations.findById(INFORMATION_MODEL, catalogId, id, mapper))

    @PreAuthorize(Authorities.WRITE)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun register(@PathVariable catalogId: String, @Valid @RequestBody values: InformationModelValues): ResponseEntity<Void> =
        operations.register(INFORMATION_MODEL, catalogId, "information-models", values, mapper)

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

    @PreAuthorize(Authorities.WRITE)
    @DeleteMapping("/{id}")
    fun delete(@PathVariable catalogId: String, @PathVariable id: String): ResponseEntity<Void> {
        operations.delete(INFORMATION_MODEL, catalogId, id)
        return ResponseEntity.noContent().build()
    }
}
