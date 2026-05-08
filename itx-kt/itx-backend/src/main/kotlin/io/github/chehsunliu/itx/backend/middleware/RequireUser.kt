package io.github.chehsunliu.itx.backend.middleware

import io.github.chehsunliu.itx.backend.respondError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext

private object RequireUserSelector : RouteSelector() {
    override suspend fun evaluate(
        context: RoutingResolveContext,
        segmentIndex: Int,
    ): RouteSelectorEvaluation = RouteSelectorEvaluation.Transparent

    override fun toString(): String = "(require-user)"
}

private val RequireUserPlugin =
    createRouteScopedPlugin("RequireUser") {
        onCall { call ->
            val ctx = call.attributes.getOrNull(ItxContext.ATTR)
            if (ctx?.userId == null) {
                call.respondError(HttpStatusCode.Unauthorized, "Unauthorized")
            }
        }
    }

/**
 * Wraps the nested routes so the auth check runs only after a handler matches the path. That keeps
 * unmatched paths returning 404 from the router (not 401 from the auth check).
 */
fun Route.requireUser(build: Route.() -> Unit): Route {
    val gated = createChild(RequireUserSelector)
    gated.install(RequireUserPlugin)
    gated.build()
    return gated
}
