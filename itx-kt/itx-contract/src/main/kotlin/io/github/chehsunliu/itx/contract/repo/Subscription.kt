package io.github.chehsunliu.itx.contract.repo

import java.util.UUID

interface SubscriptionRepo {
    /** Inserts the subscription if missing. Idempotent. */
    suspend fun subscribe(params: SubscribeParams)

    /** Removes the subscription if present. Idempotent. */
    suspend fun unsubscribe(params: UnsubscribeParams)

    /** Authors that [subscriberId] follows, ordered by most-recently subscribed first. */
    suspend fun listAuthors(subscriberId: UUID): List<User>

    /** Users subscribed to [authorId], ordered by most-recently subscribed first. */
    suspend fun listSubscribers(authorId: UUID): List<User>

    data class SubscribeParams(
        val subscriberId: UUID,
        val authorId: UUID,
    )

    data class UnsubscribeParams(
        val subscriberId: UUID,
        val authorId: UUID,
    )
}

interface RepoFactory {
    fun createPostRepo(): PostRepo

    fun createUserRepo(): UserRepo

    fun createSubscriptionRepo(): SubscriptionRepo
}
