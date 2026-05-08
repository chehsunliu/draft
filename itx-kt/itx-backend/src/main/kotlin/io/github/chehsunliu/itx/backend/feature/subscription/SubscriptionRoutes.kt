package io.github.chehsunliu.itx.backend.feature.subscription

import io.github.chehsunliu.itx.backend.error.BackendException
import io.github.chehsunliu.itx.backend.middleware.itxContext
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo
import io.github.chehsunliu.itx.contract.repo.UserRepo
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import java.util.UUID

fun Route.subscriptionRoutes(
    userRepo: UserRepo,
    subscriptionRepo: SubscriptionRepo,
) {
    route("/subscriptions") {
        put("/{authorId}") {
            val ctx = call.itxContext()
            val authorId = UUID.fromString(call.parameters.getOrFail("authorId"))
            if (ctx.userId == authorId) throw BackendException.badRequest("cannot subscribe to yourself")
            val email = ctx.userEmail ?: throw BackendException.unknown("missing X-Itx-User-Email")
            // Pre-check the target so we return 404 cleanly instead of an FK violation.
            userRepo.get(authorId)
            // Ensure the subscriber row exists; safe to call before /me has ever been hit.
            userRepo.upsert(UserRepo.UpsertParams(ctx.userId!!, email))
            subscriptionRepo.subscribe(SubscriptionRepo.SubscribeParams(ctx.userId!!, authorId))
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/{authorId}") {
            val ctx = call.itxContext()
            val authorId = UUID.fromString(call.parameters.getOrFail("authorId"))
            if (ctx.userId == authorId) throw BackendException.badRequest("cannot unsubscribe from yourself")
            userRepo.get(authorId)
            subscriptionRepo.unsubscribe(SubscriptionRepo.UnsubscribeParams(ctx.userId!!, authorId))
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
