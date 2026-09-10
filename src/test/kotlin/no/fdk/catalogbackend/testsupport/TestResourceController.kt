package no.fdk.catalogbackend.testsupport

import no.fdk.catalogbackend.security.Authorities
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * A test-only stand-in for the resource controllers.
 */
@RestController
@RequestMapping("/internal/test/{catalogId}")
class TestResourceController {
    @PreAuthorize(Authorities.READ)
    @GetMapping
    fun read(@PathVariable catalogId: String): ResponseEntity<String> = ResponseEntity.ok("read")

    @PreAuthorize(Authorities.WRITE)
    @PostMapping
    fun write(@PathVariable catalogId: String): ResponseEntity<String> = ResponseEntity.ok("write")

    @PreAuthorize(Authorities.ADMIN)
    @DeleteMapping
    fun admin(@PathVariable catalogId: String): ResponseEntity<String> = ResponseEntity.ok("admin")
}
