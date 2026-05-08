package io.github.chehsunliu.itx.backend.error

import io.ktor.http.HttpStatusCode

class BackendException(
    val status: HttpStatusCode,
    message: String,
) : RuntimeException(message) {
    companion object {
        fun notFound() = BackendException(HttpStatusCode.NotFound, "not found")

        fun badRequest(message: String) = BackendException(HttpStatusCode.BadRequest, message)

        fun unknown(message: String) = BackendException(HttpStatusCode.InternalServerError, message)
    }
}
