package no.fdk.catalogbackend.core.spi

import no.fdk.catalogbackend.core.model.ResourceType
import org.springframework.stereotype.Component

/**
 * Indexes every resource type's contributions and validates them at startup.
 */
@Component
class ResourceRegistry(
    stores: List<ResourceStore>,
    metadata: List<ResourceTypeMetadata>,
    mappers: List<ResourceMapper<*, *>>,
    rdfWriters: List<ResourceRdfWriter>,
) {
    private val storesByType = stores.indexUnique("store") { it.resourceType }
    private val metadataByType = metadata.indexUnique("metadata") { it.resourceType }
    private val mappersByType = mappers.indexUnique("mapper") { it.resourceType }
    private val rdfWritersByType = rdfWriters.indexUnique("rdf writer") { it.resourceType }
    private val typesByPathSegment = metadataByType
        .map { (type, meta) -> meta.pathSegment to type }
        .also { pairs ->
            val duplicates = pairs.groupBy { it.first }.filterValues { it.size > 1 }.keys
            require(duplicates.isEmpty()) {
                "More than one resource type registered for path segment(s) ${duplicates.joinToString()}"
            }
        }
        .toMap()

    /** Every type known to the application, in a stable order. */
    val resourceTypes: List<ResourceType> = storesByType.keys.sortedBy { it.key }

    init {
        val declared = storesByType.keys + metadataByType.keys + mappersByType.keys + rdfWritersByType.keys
        val incomplete = declared
            .associateWith { type ->
                listOfNotNull(
                    "store".takeUnless { storesByType.containsKey(type) },
                    "metadata".takeUnless { metadataByType.containsKey(type) },
                    "mapper".takeUnless { mappersByType.containsKey(type) },
                    "rdf writer".takeUnless { rdfWritersByType.containsKey(type) },
                )
            }.filterValues { it.isNotEmpty() }

        require(incomplete.isEmpty()) {
            val detail = incomplete.entries.joinToString("; ") { (type, missing) -> "$type is missing ${missing.joinToString(", ")}" }
            "Incomplete resource type registration: $detail"
        }
    }

    fun store(resourceType: ResourceType): ResourceStore = storesByType.require(resourceType, "store")

    fun metadata(resourceType: ResourceType): ResourceTypeMetadata = metadataByType.require(resourceType, "metadata")

    fun mapper(resourceType: ResourceType): ResourceMapper<*, *> = mappersByType.require(resourceType, "mapper")

    fun rdfWriter(resourceType: ResourceType): ResourceRdfWriter = rdfWritersByType.require(resourceType, "rdf writer")

    fun resourceTypeForPathSegment(pathSegment: String): ResourceType? = typesByPathSegment[pathSegment]

    fun stores(): Collection<ResourceStore> = storesByType.values

    private fun <T> Map<ResourceType, T>.require(resourceType: ResourceType, kind: String): T =
        this[resourceType] ?: throw IllegalArgumentException("No $kind registered for resource type $resourceType")

    private fun <T> List<T>.indexUnique(kind: String, keySelector: (T) -> ResourceType): Map<ResourceType, T> {
        val duplicates = groupBy(keySelector).filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "More than one $kind registered for resource type(s) ${duplicates.joinToString()}" }
        return associateBy(keySelector)
    }
}
