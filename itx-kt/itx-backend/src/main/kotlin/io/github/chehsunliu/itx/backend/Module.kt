package io.github.chehsunliu.itx.backend

import io.github.chehsunliu.itx.backend.error.BackendException
import io.github.chehsunliu.itx.backend.feature.health.healthRoutes
import io.github.chehsunliu.itx.backend.feature.post.postRoutes
import io.github.chehsunliu.itx.backend.feature.subscription.subscriptionRoutes
import io.github.chehsunliu.itx.backend.feature.user.userRoutes
import io.github.chehsunliu.itx.backend.middleware.installItxContext
import io.github.chehsunliu.itx.backend.middleware.requireUser
import io.github.chehsunliu.itx.contract.queue.QueueException
import io.github.chehsunliu.itx.contract.repo.RepoNotFoundException
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

internal val itxJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

fun Application.itxModule(state: AppState) {
    install(ContentNegotiation) { json(itxJson) }
    install(StatusPages) {
        exception<BackendException> { call, cause -> call.respondError(cause.status, cause.message ?: "") }
        exception<RepoNotFoundException> { call, _ -> call.respondError(HttpStatusCode.NotFound, "not found") }
        exception<QueueException> { call, cause ->
            call.respondError(HttpStatusCode.InternalServerError, cause.message ?: "queue error")
        }
        exception<Throwable> { call, cause ->
            call.respondError(HttpStatusCode.InternalServerError, cause.message ?: "internal error")
        }
        status(HttpStatusCode.NotFound) { call, _ -> call.respondError(HttpStatusCode.NotFound, "Not Found") }
        status(HttpStatusCode.MethodNotAllowed) { call, _ ->
            call.respondError(HttpStatusCode.MethodNotAllowed, "Method Not Allowed")
        }
    }
    installItxContext()

    routing {
        route("/api/v1") {
            healthRoutes()
            requireUser {
                postRoutes(state.postRepo, state.controlStandardQueue)
                userRoutes(state.userRepo, state.subscriptionRepo)
                subscriptionRoutes(state.userRepo, state.subscriptionRepo)
            }
        }
    }
}

internal suspend fun ApplicationCall.respondError(
    status: HttpStatusCode,
    message: String,
) {
    val payload: JsonObject =
        buildJsonObject {
            put("error", buildJsonObject { put("message", JsonPrimitive(message)) })
        }
    val body = itxJson.encodeToString(JsonObject.serializer(), payload)
    respond(TextContent(body, ContentType.Application.Json, status))
}
