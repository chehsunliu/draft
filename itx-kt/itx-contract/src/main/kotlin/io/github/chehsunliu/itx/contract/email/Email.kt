package io.github.chehsunliu.itx.contract.email

import kotlinx.serialization.Serializable

class EmailException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

@Serializable data class SendEmailMessage(
    val to: String,
    val subject: String,
    val body: String,
)

interface EmailClient {
    /** Sends a single email. Returns normally on a 2xx response from the upstream provider. */
    suspend fun send(msg: SendEmailMessage)
}
