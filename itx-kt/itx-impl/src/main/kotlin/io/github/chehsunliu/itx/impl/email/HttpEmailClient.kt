package io.github.chehsunliu.itx.impl.email

import io.github.chehsunliu.itx.contract.email.EmailClient
import io.github.chehsunliu.itx.contract.email.EmailException
import io.github.chehsunliu.itx.contract.email.SendEmailMessage
import io.github.chehsunliu.itx.impl.requireEnv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class HttpEmailClient(
    private val url: String,
    private val apiKey: String,
    private val client: HttpClient =
        HttpClient(CIO) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        },
) : EmailClient {
    companion object {
        fun fromEnv(): HttpEmailClient =
            HttpEmailClient(
                url = requireEnv("ITX_EMAIL_URL"),
                apiKey = requireEnv("ITX_EMAIL_API_KEY"),
            )
    }

    override suspend fun send(msg: SendEmailMessage) {
        val response =
            try {
                client.post(url) {
                    bearerAuth(apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(msg)
                }
            } catch (e: Exception) {
                throw EmailException("email API call failed", e)
            }
        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText() }.getOrNull().orEmpty()
            throw EmailException("email API returned ${response.status.value}: $body")
        }
    }
}
