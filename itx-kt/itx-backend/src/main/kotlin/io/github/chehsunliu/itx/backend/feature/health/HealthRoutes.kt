package io.github.chehsunliu.itx.backend.feature.health

import io.github.chehsunliu.itx.backend.middleware.respondDataObject
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

fun Route.healthRoutes() {
    route("/health") {
        get {
            call.respondDataObject(buildJsonObject { put("status", JsonPrimitive("ok")) })
        }
    }
}
