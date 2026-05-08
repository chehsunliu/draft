package io.github.chehsunliu.itx.backend.middleware

import io.github.chehsunliu.itx.backend.error.BackendException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.util.AttributeKey
import java.util.UUID

data class ItxContext(
    val requestId: UUID,
    val userId: UUID?,
    val userEmail: String?,
) {
    companion object {
        const val HEADER_REQUEST_ID = "X-Itx-Request-Id"
        const val HEADER_USER_ID = "X-Itx-User-Id"
        const val HEADER_USER_EMAIL = "X-Itx-User-Email"

        val ATTR = AttributeKey<ItxContext>("itx.context")
    }
}

fun Application.installItxContext() {
    intercept(ApplicationCallPipeline.Plugins) {
        val requestId =
            try {
                call.request.header(ItxContext.HEADER_REQUEST_ID)?.let(UUID::fromString)
                    ?: UUID.randomUUID()
            } catch (_: IllegalArgumentException) {
                throw BackendException(HttpStatusCode.BadRequest, "invalid ${ItxContext.HEADER_REQUEST_ID}")
            }
        val userId =
            try {
                call.request.header(ItxContext.HEADER_USER_ID)?.let(UUID::fromString)
            } catch (_: IllegalArgumentException) {
                throw BackendException(HttpStatusCode.BadRequest, "invalid ${ItxContext.HEADER_USER_ID}")
            }
        val userEmail = call.request.header(ItxContext.HEADER_USER_EMAIL)
        call.attributes.put(ItxContext.ATTR, ItxContext(requestId, userId, userEmail))
    }
}

fun io.ktor.server.application.ApplicationCall.itxContext(): ItxContext = attributes[ItxContext.ATTR]
