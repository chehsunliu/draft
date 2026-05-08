package io.github.chehsunliu.itx.worker

import io.github.chehsunliu.itx.contract.queue.MessageHandler
import io.github.chehsunliu.itx.contract.queue.MessageQueue
import io.github.chehsunliu.itx.contract.queue.MessageQueueFactory
import io.github.chehsunliu.itx.impl.email.HttpEmailClient
import io.github.chehsunliu.itx.impl.queue.rabbitmq.RabbitMessageQueueFactory
import io.github.chehsunliu.itx.impl.queue.sqs.SqsMessageQueueFactory
import io.github.chehsunliu.itx.impl.repo.mariadb.MariaDbRepoFactory
import io.github.chehsunliu.itx.impl.repo.postgres.PostgresRepoFactory
import io.github.chehsunliu.itx.worker.compute.ComputeDispatcher
import io.github.chehsunliu.itx.worker.control.ControlDispatcher
import io.github.chehsunliu.itx.worker.run.runQueueLoops
import kotlinx.coroutines.runBlocking
import sun.misc.Signal

private fun queueFactory(): MessageQueueFactory =
    when (val provider = System.getenv("ITX_QUEUE_PROVIDER") ?: "sqs") {
        "sqs" -> SqsMessageQueueFactory.fromEnv()
        "rabbitmq" -> RabbitMessageQueueFactory.fromEnv()
        else -> error("unknown ITX_QUEUE_PROVIDER: $provider")
    }

fun main(argv: Array<String>) {
    var mode = "control"
    var i = 0
    while (i < argv.size) {
        val a = argv[i]
        when {
            a.startsWith("--mode=") -> mode = a.removePrefix("--mode=")
            a == "--mode" -> mode = argv[++i]
            a == "--log-level" -> {
                System.setProperty("itx.log.level", argv[++i])
            }
            else -> {}
        }
        i++
    }

    val queueFactory = queueFactory()
    val (queues, handler) = wire(mode, queueFactory)
    runBlocking { runQueueLoops(queues, handler, registerSignals = true) }
    Signal.handle(Signal("INT")) { Runtime.getRuntime().halt(0) }
}

private fun wire(
    mode: String,
    factory: MessageQueueFactory,
): Pair<List<MessageQueue>, MessageHandler> =
    when (mode) {
        "control" -> {
            val repoFactory =
                when (val provider = System.getenv("ITX_DB_PROVIDER") ?: "postgres") {
                    "postgres" -> PostgresRepoFactory.fromEnv()
                    "mariadb" -> MariaDbRepoFactory.fromEnv()
                    else -> error("unknown ITX_DB_PROVIDER: $provider")
                }
            val dispatcher =
                ControlDispatcher(
                    postRepo = repoFactory.createPostRepo(),
                    userRepo = repoFactory.createUserRepo(),
                    subscriptionRepo = repoFactory.createSubscriptionRepo(),
                    emailClient = HttpEmailClient.fromEnv(),
                )
            val queues =
                listOf(factory.createControlStandardQueue(), factory.createControlPremiumQueue())
            queues to dispatcher
        }

        "compute" -> {
            val dispatcher = ComputeDispatcher()
            val queues =
                listOf(factory.createComputeStandardQueue(), factory.createComputePremiumQueue())
            queues to dispatcher
        }

        else -> error("unknown --mode: $mode (expected control|compute)")
    }
