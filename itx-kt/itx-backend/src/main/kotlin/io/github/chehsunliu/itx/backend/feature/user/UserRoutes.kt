package io.github.chehsunliu.itx.backend.feature.user

import io.github.chehsunliu.itx.backend.error.BackendException
import io.github.chehsunliu.itx.backend.feature.UuidSerializer
import io.github.chehsunliu.itx.backend.middleware.itxContext
import io.github.chehsunliu.itx.backend.middleware.respondData
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo
import io.github.chehsunliu.itx.contract.repo.User
import io.github.chehsunliu.itx.contract.repo.UserRepo
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class UserDto(
    @Serializable(with = UuidSerializer::class) val id: UUID,
    val email: String,
)

fun User.toDto(): UserDto = UserDto(id, email)

@Serializable data class ListSubscriptionsResponse(
    val items: List<UserDto>,
)

fun Route.userRoutes(
    userRepo: UserRepo,
    subscriptionRepo: SubscriptionRepo,
) {
    route("/users") {
        get("/me") {
            val ctx = call.itxContext()
            val email = ctx.userEmail ?: throw BackendException.unknown("missing X-Itx-User-Email")
            val user = userRepo.upsert(UserRepo.UpsertParams(ctx.userId!!, email))
            call.respondData(UserDto.serializer(), user.toDto())
        }

        get("/{id}/subscriptions") {
            val id = UUID.fromString(call.parameters.getOrFail("id"))
            // Pre-check the subject so an unknown user yields 404, not an empty list.
            userRepo.get(id)
            val authors = subscriptionRepo.listAuthors(id)
            call.respondData(
                ListSubscriptionsResponse.serializer(),
                ListSubscriptionsResponse(items = authors.map { it.toDto() }),
            )
        }
    }
}
