package no.fdk.catalogbackend.core.persistence

import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@MappedSuperclass
abstract class CatalogResourceEntity {
    @Id
    var id: String = ""

    var catalogId: String = ""

    var published: Boolean = false

    var publishedDate: Instant? = null

    var created: Instant = Instant.EPOCH

    var lastModified: Instant? = null

    var uri: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    var data: Map<String, Any?>? = null
}
