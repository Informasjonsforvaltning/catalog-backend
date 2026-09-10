package no.fdk.catalogbackend.utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import no.fdk.catalogbackend.utils.jwt.JwkStore

private val mockserver = WireMockServer(5050)

fun startMockServer() {
    if (!mockserver.isRunning) {
        mockserver.stubFor(
            get(urlEqualTo("/auth/realms/fdk/protocol/openid-connect/certs"))
                .willReturn(okJson(JwkStore.jwks())),
        )

        mockserver.start()
    }
}

fun stopMockServer() {
    if (mockserver.isRunning) mockserver.stop()
}
