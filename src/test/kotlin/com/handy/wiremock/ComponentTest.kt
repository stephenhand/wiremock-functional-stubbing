package com.handy.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import wiremock.org.eclipse.jetty.http.HttpStatus

val server: WireMockServer = WireMockServer(
    WireMockConfiguration()
        .extensions(FunctionalResponseDefinitionTransformer())

).apply {
    start()
    RestAssured.port = port()
    RestAssured.baseURI = "http://localhost"
}

private val ENDPOINT = "/awesome/stuff/here"

private fun String.makeShoutier(requiredShoutyness: Int) = toUpperCase().replace("!", "!".repeat(requiredShoutyness))

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComponentTest {


    @Test
    fun `Result of provided transform function is returned in stub response`() {
        server.stubFor(post(ENDPOINT)
            .willReturn(
                aResponse()
                    .withFunctionalTransformer { request, responseDefinition -> ResponseDefinitionBuilder
                        .like(responseDefinition)
                        .withBody(request.bodyAsString!!.makeShoutier(2))
                        .withStatus(500)
                        .build()
                    }
                    .withStatus(200)
                    .withBody("It worked!")
            )
        )

        val responseContent = given()
            .body("It didn't really work!")
            .contentType(ContentType.TEXT)
            .`when`()
            .post(ENDPOINT)
            .then()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500)
            .extract()
            .asString()
        Assertions.assertEquals("IT DIDN'T REALLY WORK!!", responseContent)
    }

    @Test
    fun `Serially executing the same stub returns correct response for each`() {
        server.stubFor(post(ENDPOINT)
            .willReturn(
                aResponse()
                    .withFunctionalTransformer { request, responseDefinition -> ResponseDefinitionBuilder
                        .like(responseDefinition)
                        .withBody(request.bodyAsString!!.makeShoutier(request.getHeader("stub-call-index").toString().toInt()))
                        .withStatus(500)
                        .build()
                    }
                    .withStatus(200)
                    .withBody("It worked!")
            )
        )
        repeat(100) {

            val responseContent = given()
                .body("It didn't really work!")
                .header("stub-call-index", it)
                .contentType(ContentType.TEXT)
                .`when`()
                .post(ENDPOINT)
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500)
                .extract()
                .asString()
            Assertions.assertEquals("IT DIDN'T REALLY WORK${"!".repeat(it)}", responseContent)
        }
    }
    @Test
    fun `Concurrently executing the same stub returns correct response for each`() {
        server.stubFor(post(ENDPOINT)
            .willReturn(
                aResponse()
                    .withFunctionalTransformer { request, responseDefinition -> ResponseDefinitionBuilder
                        .like(responseDefinition)
                        .withBody(request.bodyAsString!!.toUpperCase().replace("!", "!".repeat(request.getHeader("stub-call-index").toString().toInt())))
                        .withStatus(500)
                        .build()
                    }
                    .withStatus(200)
                    .withBody("It worked!")
            )
        )

        runBlocking {
            repeat(100) {
                launch(Dispatchers.IO) {

                    val responseContent = given()
                        .body("It didn't really work!")
                        .header("stub-call-index", it)
                        .contentType(ContentType.TEXT)
                        .`when`()
                        .post(ENDPOINT)
                        .then()
                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500)
                        .extract()
                        .asString()
                    Assertions.assertEquals("IT DIDN'T REALLY WORK${"!".repeat(it)}", responseContent)
                }
            }
        }
    }
}