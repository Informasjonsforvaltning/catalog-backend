package no.fdk.catalogbackend.utils.jwt

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.util.Date

const val TEST_ISSUER = "http://localhost:5050/auth/realms/fdk"
const val TEST_AUDIENCE = "catalog-backend"

const val CATALOG_ID = "910244132"
const val OTHER_CATALOG_ID = "123456789"

class JwtToken(private val access: Access, private val audience: String = TEST_AUDIENCE, private val issuer: String = TEST_ISSUER) {
    private fun buildToken(): String {
        val claims = JWTClaimsSet.Builder()
            .audience(audience)
            .issuer(issuer)
            .expirationTime(Date(Date().time + 120 * 1000))
            .claim("user_name", "1924782563")
            .claim("name", "TEST USER")
            .claim("authorities", access.authorities)
            .build()

        return SignedJWT(JwkStore.jwtHeader(), claims)
            .apply { sign(JwkStore.signer()) }
            .serialize()
    }

    override fun toString(): String = buildToken()
}

enum class Access(val authorities: String) {
    ORG_READ("organization:$CATALOG_ID:read"),
    ORG_WRITE("organization:$CATALOG_ID:write"),
    ORG_ADMIN("organization:$CATALOG_ID:admin"),
    ROOT("system:root:admin"),
    WRONG_ORG_WRITE("organization:$OTHER_CATALOG_ID:write"),
    MULTIPLE_ORGS("organization:$OTHER_CATALOG_ID:admin,organization:$CATALOG_ID:read"),
}
