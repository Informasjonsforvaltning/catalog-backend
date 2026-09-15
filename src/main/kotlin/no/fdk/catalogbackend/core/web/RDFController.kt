package no.fdk.catalogbackend.core.web

import no.fdk.catalogbackend.core.rdf.RdfService
import no.fdk.catalogbackend.core.rdf.jenaLangFromAcceptHeader
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
@RequestMapping(
    value = ["/graphs"],
    produces = [
        "text/turtle",
        "application/rdf+json",
        "application/rdf+xml",
        "application/ld+json",
        "application/n-triples",
        "application/n-quads",
        "application/trig",
        "application/trix",
    ],
)
class RDFController(private val rdfService: RdfService) {
    @GetMapping("/catalogs")
    fun getAllCatalogs(@RequestHeader(HttpHeaders.ACCEPT) accept: String?): ResponseEntity<String> =
        ResponseEntity(rdfService.serializeAll(jenaLangFromAcceptHeader(accept)), HttpStatus.OK)

    @GetMapping("/catalogs/{catalogId}")
    fun getCatalog(@RequestHeader(HttpHeaders.ACCEPT) accept: String?, @PathVariable catalogId: String): ResponseEntity<String> =
        ResponseEntity(rdfService.serializeCatalog(catalogId, jenaLangFromAcceptHeader(accept)), HttpStatus.OK)

    @GetMapping("/{pathSegment}/{id}")
    fun getResource(
        @RequestHeader(HttpHeaders.ACCEPT) accept: String?,
        @PathVariable pathSegment: String,
        @PathVariable id: String,
    ): ResponseEntity<String> =
        ResponseEntity(rdfService.serializeResource(pathSegment, id, jenaLangFromAcceptHeader(accept)), HttpStatus.OK)
}
