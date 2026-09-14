package no.fdk.catalogbackend.core.rdf

import no.fdk.catalogbackend.core.model.ContactPoint
import no.fdk.catalogbackend.core.model.LocalizedStrings
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.rdf.model.Literal
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.VCARD4
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.io.StringWriter
import java.net.URI

fun Resource.safeAddLocalizedString(property: Property, langMap: LocalizedStrings?): Resource {
    langMap?.nb?.let { safeAddLangLiteral(property, it, "nb") }
    langMap?.nn?.let { safeAddLangLiteral(property, it, "nn") }
    langMap?.en?.let { safeAddLangLiteral(property, it, "en") }
    langMap?.no?.let { safeAddLangLiteral(property, it, "no") }
    return this
}

fun Resource.safeAddStringLiteral(property: Property, value: String?): Resource = if (value.isNullOrEmpty()) {
    this
} else {
    addLiteral(property, value)
}

fun Resource.safeAddLiteral(property: Property, value: Literal?): Resource = if (value == null) {
    this
} else {
    addLiteral(property, value)
}

fun Resource.safeAddLangLiteral(property: Property, value: String?, lang: String): Resource = if (value.isNullOrEmpty()) {
    this
} else {
    addLiteral(property, model.createLiteral(value, lang))
}

fun Resource.safeAddProperty(property: Property, value: String?): Resource = if (value.isNullOrEmpty()) {
    this
} else {
    addProperty(property, value)
}

fun Resource.safeAddProperty(property: Property, value: Resource?): Resource = if (value == null) {
    this
} else {
    addProperty(property, value)
}

fun Resource.safeAddLinkedProperty(property: Property, value: String?): Resource = if (value.isNullOrEmpty()) {
    this
} else {
    addProperty(property, model.createResource(value))
}

fun Resource.safeAddFlexibleDateLiteral(property: Property, value: String?): Resource {
    if (value.isNullOrEmpty()) return this
    val xsdType = when (value.length) {
        4 -> XSDDatatype.XSDgYear
        7 -> XSDDatatype.XSDgYearMonth
        10 -> XSDDatatype.XSDdate
        else -> return this
    }
    if (!xsdType.isValid(value)) return this
    return safeAddLiteral(property, model.createTypedLiteral(value, xsdType))
}

fun Resource.addContactPoints(contactPoints: List<ContactPoint>?): Resource {
    contactPoints?.forEach {
        val resource = model
            .safeCreateResource()
            .addProperty(RDF.type, VCARD4.Organization)
            .safeAddLocalizedString(VCARD4.fn, it.name)
            .safeAddLinkedProperty(VCARD4.hasURL, it.url.takeIf { url -> url.isValidURI() })
            .safeAddLinkedProperty(VCARD4.hasEmail, it.email?.addContactStringPrefix("mailto:"))
        if (!it.telephone.isNullOrBlank()) {
            resource.addProperty(VCARD4.hasTelephone, model.telephoneResource(it.telephone))
        }
        addProperty(DCAT.contactPoint, resource)
    }
    return this
}

fun String.addContactStringPrefix(prefix: String): String? = when {
    startsWith(prefix) -> trim()
    isNotEmpty() -> prefix + trim()
    else -> null
}

fun Model.telephoneResource(telephone: String): Resource = telephone
    .trim { it <= ' ' }
    .filterIndexed { index, c ->
        when {
            index == 0 && c == '+' -> true
            c in '0'..'9' -> true
            else -> false
        }
    }.let { createResource("tel:$it") }

fun Model.createRDFResponse(lang: Lang): String = StringWriter().use { out ->
    write(out, lang.name)
    out.toString()
}

fun Model.safeCreateResource(value: String? = null): Resource = try {
    value
        ?.let(::URI)
        ?.takeIf { it.isAbsolute && !it.isOpaque && !it.host.isNullOrEmpty() }
        ?.let { createResource(value) }
        ?: createResource()
} catch (_: Exception) {
    createResource()
}

fun String?.isValidURI(): Boolean = this != null &&
    runCatching {
        URI(this).let { it.isAbsolute && !it.isOpaque && !it.host.isNullOrEmpty() }
    }.getOrDefault(false)

/**
 * Map an Accept header to a Jena [Lang], defaults to Turtle. Unknown values yield HTTP 406.
 */
fun jenaLangFromAcceptHeader(accept: String?): Lang = when {
    accept == null -> Lang.TURTLE
    accept.contains(Lang.TURTLE.headerString) -> Lang.TURTLE
    accept.contains(Lang.RDFXML.headerString) -> Lang.RDFXML
    accept.contains(Lang.RDFJSON.headerString) -> Lang.RDFJSON
    accept.contains(Lang.JSONLD.headerString) -> Lang.JSONLD
    accept.contains(Lang.NTRIPLES.headerString) -> Lang.NTRIPLES
    accept.contains(Lang.NQUADS.headerString) -> Lang.NQUADS
    accept.contains(Lang.TRIG.headerString) -> Lang.TRIG
    accept.contains(Lang.TRIX.headerString) -> Lang.TRIX
    accept.contains("*/*") -> Lang.TURTLE
    else -> throw ResponseStatusException(HttpStatus.NOT_ACCEPTABLE)
}
