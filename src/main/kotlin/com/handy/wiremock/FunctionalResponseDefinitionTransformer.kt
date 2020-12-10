package com.handy.wiremock

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.ResponseDefinition
import java.lang.ref.WeakReference
import java.util.*

typealias Transformer = (Request, ResponseDefinition)->ResponseDefinition

internal const val TRANSFORM_FUNCTION_PARAMETER = "transform"

fun responseWithFunctionalTransformer(builder: ResponseDefinitionBuilder, responseTransformer: Transformer): ResponseDefinitionBuilder {
    return builder
        .withTransformer("functional-transformer", TRANSFORM_FUNCTION_PARAMETER, responseTransformer)

}

fun ResponseDefinitionBuilder.withFunctionalTransformer(responseGenerator: Transformer) = responseWithFunctionalTransformer(this, responseGenerator)

class FunctionalResponseDefinitionTransformer : ResponseDefinitionTransformer() {

    override fun getName(): String = "functional-transformer"

    override fun transform(
        request: Request,
        responseDefinition: ResponseDefinition,
        files: FileSource,
        parameters: Parameters
    ): ResponseDefinition {
        @Suppress("UNCHECKED_CAST")
        return (parameters[TRANSFORM_FUNCTION_PARAMETER] as Transformer)(request, responseDefinition)
    }
}