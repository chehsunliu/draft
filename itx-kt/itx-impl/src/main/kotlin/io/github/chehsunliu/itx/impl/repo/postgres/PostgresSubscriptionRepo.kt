package io.github.chehsunliu.itx.impl.repo.postgres

import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo
import io.github.chehsunliu.itx.contract.repo.User
import io.github.chehsunliu.itx.impl.repo.bindAll
import io.github.chehsunliu.itx.impl.repo.mapAll
import io.github.chehsunliu.itx.impl.repo.useConnection
import java.util.UUID
import javax.sql.DataSource

internal class PostgresSubscriptionRepo(
    private val ds: DataSource,
) : SubscriptionRepo {
    override suspend fun subscribe(params: SubscriptionRepo.SubscribeParams) {
        ds.useConnection { conn ->
            conn
                .prepareStatement(
                    """
                INSERT INTO subscriptions (subscriber_id, author_id) VALUES (?, ?)
                ON CONFLICT (subscriber_id, author_id) DO NOTHING
                """,
                ).use {
                    it.bindAll(params.subscriberId, params.authorId)
                    it.executeUpdate()
                }
        }
    }

    override suspend fun unsubscribe(params: SubscriptionRepo.UnsubscribeParams) {
        ds.useConnection { conn ->
            conn
                .prepareStatement(
                    "DELETE FROM subscriptions WHERE subscriber_id = ? AND author_id = ?",
                ).use {
                    it.bindAll(params.subscriberId, params.authorId)
                    it.executeUpdate()
                }
        }
    }

    override suspend fun listAuthors(subscriberId: UUID): List<User> =
        ds.useConnection { conn ->
            conn
                .prepareStatement(
                    """
                SELECT u.id, u.email
                FROM subscriptions s JOIN users u ON u.id = s.author_id
                WHERE s.subscriber_id = ?
                ORDER BY s.created_at DESC, u.id ASC
                """,
                ).use { ps ->
                    ps.bindAll(subscriberId)
                    ps.executeQuery().use { rs ->
                        rs.mapAll { User(it.getObject("id", UUID::class.java), it.getString("email")) }
                    }
                }
        }

    override suspend fun listSubscribers(authorId: UUID): List<User> =
        ds.useConnection { conn ->
            conn
                .prepareStatement(
                    """
                SELECT u.id, u.email
                FROM subscriptions s JOIN users u ON u.id = s.subscriber_id
                WHERE s.author_id = ?
                ORDER BY s.created_at DESC, u.id ASC
                """,
                ).use { ps ->
                    ps.bindAll(authorId)
                    ps.executeQuery().use { rs ->
                        rs.mapAll { User(it.getObject("id", UUID::class.java), it.getString("email")) }
                    }
                }
        }
}
