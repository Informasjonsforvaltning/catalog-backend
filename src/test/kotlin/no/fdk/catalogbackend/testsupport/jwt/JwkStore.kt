package no.fdk.catalogbackend.testsupport.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import java.util.UUID

object JwkStore {
    private val jwk: RSAKey = RSAKeyGenerator(2048)
        .algorithm(JWSAlgorithm.RS256)
        .keyUse(KeyUse.SIGNATURE)
        .keyID(UUID.randomUUID().toString())
        .generate()

    fun jwks(): String = JWKSet(jwk.toPublicJWK()).toString()

    fun jwtHeader(): JWSHeader = JWSHeader.Builder(JWSAlgorithm.RS256).keyID(jwk.keyID).build()

    fun signer(): RSASSASigner = RSASSASigner(jwk)
}
