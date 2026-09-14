package no.fdk.catalogbackend.testsupport

import no.fdk.catalogbackend.core.rdf.createRDFResponse
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.slf4j.Logger
import java.io.StringReader

fun checkIfIsomorphicAndPrintDiff(actual: Model, expected: Model, name: String, logger: Logger): Boolean {
    val parsedActual = ModelFactory.createDefaultModel().read(StringReader(actual.createRDFResponse(Lang.TURTLE)), null, "TURTLE")
    val parsedExpected = ModelFactory.createDefaultModel().read(StringReader(expected.createRDFResponse(Lang.TURTLE)), null, "TURTLE")

    val isIsomorphic = parsedActual.isIsomorphicWith(parsedExpected)

    if (!isIsomorphic) {
        val actualDiff = parsedActual.difference(parsedExpected).createRDFResponse(Lang.TURTLE)
        val expectedDiff = parsedExpected.difference(parsedActual).createRDFResponse(Lang.TURTLE)

        if (actualDiff.isNotEmpty()) {
            logger.error("non expected nodes in $name:")
            logger.error(actualDiff)
        }
        if (expectedDiff.isNotEmpty()) {
            logger.error("missing nodes in $name:")
            logger.error(expectedDiff)
        }
    }
    return isIsomorphic
}

fun loadTurtle(resourcePath: String): Model = ModelFactory.createDefaultModel().read(
    requireNotNull(object {}.javaClass.classLoader.getResourceAsStream(resourcePath)) {
        "Missing classpath resource: $resourcePath"
    },
    null,
    "TURTLE",
)
