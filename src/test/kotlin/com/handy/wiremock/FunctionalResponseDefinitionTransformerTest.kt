package com.handy.wiremock

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.http.Request
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class FunctionalResponseDefinitionTransformerTest {

    @Test
    fun `Runs base response through transform function passing in request and returns result`() {
        val mockRequest: Request = mockk()
        every { mockRequest.bodyAsString } returns "It didn't really work!"
        val transformer: Transformer = { request, responseDefinition -> ResponseDefinitionBuilder
            .like(responseDefinition)
            .withBody(request.bodyAsString!!.toUpperCase().replace("!", "!!"))
            .withStatus(500)
            .build()
        }
        val baseResponse = aResponse()
            .withFunctionalTransformer(transformer)
            .withStatus(200)
            .withBody("It worked!")
        val transformed = FunctionalResponseDefinitionTransformer().transform(mockRequest, baseResponse.build(), mockk(), Parameters.from(mapOf(TRANSFORM_FUNCTION_PARAMETER to transformer)))
        Assertions.assertEquals("IT DIDN'T REALLY WORK!!", transformed.body)
        Assertions.assertEquals(500, transformed.status)
    }
}