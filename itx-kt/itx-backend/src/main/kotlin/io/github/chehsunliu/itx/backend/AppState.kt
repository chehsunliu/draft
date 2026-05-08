package io.github.chehsunliu.itx.backend

import io.github.chehsunliu.itx.contract.queue.MessageQueue
import io.github.chehsunliu.itx.contract.repo.PostRepo
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo
import io.github.chehsunliu.itx.contract.repo.UserRepo
import io.github.chehsunliu.itx.impl.queue.rabbitmq.RabbitMessageQueueFactory
import io.github.chehsunliu.itx.impl.queue.sqs.SqsMessageQueueFactory
import io.github.chehsunliu.itx.impl.repo.mariadb.MariaDbRepoFactory
import io.github.chehsunliu.itx.impl.repo.postgres.PostgresRepoFactory

class AppState(
    val postRepo: PostRepo,
    val userRepo: UserRepo,
    val subscriptionRepo: SubscriptionRepo,
    val controlStandardQueue: MessageQueue,
) {
    fun close() { /* no-op for now; HikariCP closes via DataSource lifecycle */ }

    companion object {
        fun fromEnv(): AppState {
            val repoFactory =
                when (val provider = System.getenv("ITX_DB_PROVIDER") ?: "postgres") {
                    "postgres" -> PostgresRepoFactory.fromEnv()
                    "mariadb" -> MariaDbRepoFactory.fromEnv()
                    else -> error("unknown ITX_DB_PROVIDER: $provider")
                }
            val queueFactory =
                when (val provider = System.getenv("ITX_QUEUE_PROVIDER") ?: "sqs") {
                    "sqs" -> SqsMessageQueueFactory.fromEnv()
                    "rabbitmq" -> RabbitMessageQueueFactory.fromEnv()
                    else -> error("unknown ITX_QUEUE_PROVIDER: $provider")
                }
            return AppState(
                postRepo = repoFactory.createPostRepo(),
                userRepo = repoFactory.createUserRepo(),
                subscriptionRepo = repoFactory.createSubscriptionRepo(),
                controlStandardQueue = queueFactory.createControlStandardQueue(),
            )
        }
    }
}
