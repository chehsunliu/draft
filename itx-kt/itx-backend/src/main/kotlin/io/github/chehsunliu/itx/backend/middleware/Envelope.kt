package io.github.chehsunliu.itx.backend.middleware

import io.github.chehsunliu.itx.backend.itxJson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/**
 * Wraps a serializable payload as `{ "data": <payload> }`. Mirrors the success arm of the
 * wrap_response middleware in itx-rs.
 */
suspend fun <T> ApplicationCall.respondData(
    serializer: KSerializer<T>,
    value: T,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    val element: JsonElement = itxJson.encodeToJsonElement(serializer, value)
    val wrapped: JsonObject = buildJsonObject { put("data", element) }
    val body = itxJson.encodeToString(JsonObject.serializer(), wrapped)
    respond(TextContent(body, ContentType.Application.Json, status))
}

/**
 * Convenience for handlers that already produced a JsonObject (e.g., `Map.of("status", "ok")`
 * style responses).
 */
suspend fun ApplicationCall.respondDataObject(
    payload: JsonObject,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    val wrapped = buildJsonObject { put("data", payload) }
    val body = itxJson.encodeToString(JsonObject.serializer(), wrapped)
    respond(TextContent(body, ContentType.Application.Json, status))
}
