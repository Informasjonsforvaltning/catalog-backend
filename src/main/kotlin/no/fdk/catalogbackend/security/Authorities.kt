package no.fdk.catalogbackend.security

object Authorities {
    const val READ =
        "hasAnyAuthority('system:root:admin', 'organization:' + #catalogId + ':admin', " +
            "'organization:' + #catalogId + ':write', 'organization:' + #catalogId + ':read')"

    const val WRITE = "hasAnyAuthority('organization:' + #catalogId + ':admin', 'organization:' + #catalogId + ':write')"

    const val ADMIN = "hasAuthority('organization:' + #catalogId + ':admin')"

    const val ROOT_ADMIN = "system:root:admin"
}
