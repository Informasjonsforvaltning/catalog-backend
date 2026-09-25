package no.fdk.catalogbackend.core.model

data class LocalizedStrings(val nb: String? = null, val nn: String? = null, val en: String? = null, val no: String? = null) {
    fun hasData(): Boolean = listOfNotNull(nb, nn, en, no).any { it.isNotBlank() }
}

data class LocalizedStringLists(
    val nb: List<String>? = null,
    val nn: List<String>? = null,
    val en: List<String>? = null,
    val no: List<String>? = null,
) {
    fun hasData(): Boolean = listOfNotNull(nb, nn, en, no).any { list -> list.any { it.isNotBlank() } }
}

data class UriWithLabel(val uri: String? = null, val prefLabel: LocalizedStrings? = null)

data class ContactPoint(
    val name: LocalizedStrings? = null,
    val email: String? = null,
    val telephone: String? = null,
    val url: String? = null,
)

data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

    override fun toString(): String = "$major.$minor.$patch"
}
