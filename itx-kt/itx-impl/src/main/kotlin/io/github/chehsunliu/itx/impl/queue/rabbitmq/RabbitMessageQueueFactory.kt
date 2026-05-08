package io.github.chehsunliu.itx.impl.queue.rabbitmq

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import io.github.chehsunliu.itx.contract.queue.MessageQueue
import io.github.chehsunliu.itx.contract.queue.MessageQueueFactory
import io.github.chehsunliu.itx.contract.queue.QueueException
import io.github.chehsunliu.itx.impl.envInt
import io.github.chehsunliu.itx.impl.requireEnv

class RabbitMessageQueueFactory(
    private val connection: Connection,
    private val maxConcurrency: Int,
    private val controlStandardQueue: String,
    private val controlPremiumQueue: String,
    private val computeStandardQueue: String,
    private val computePremiumQueue: String,
) : MessageQueueFactory {
    companion object {
        fun fromEnv(): RabbitMessageQueueFactory {
            val factory =
                ConnectionFactory().apply {
                    host = requireEnv("ITX_RABBITMQ_HOST")
                    port = requireEnv("ITX_RABBITMQ_PORT").toInt()
                    username = requireEnv("ITX_RABBITMQ_USER")
                    password = requireEnv("ITX_RABBITMQ_PASSWORD")
                }
            val conn =
                try {
                    factory.newConnection("itx-kt")
                } catch (e: Exception) {
                    throw QueueException("failed to open RabbitMQ connection", e)
                }
            return RabbitMessageQueueFactory(
                connection = conn,
                maxConcurrency = envInt("ITX_RABBITMQ_MAX_CONCURRENCY", 100),
                controlStandardQueue = requireEnv("ITX_RABBITMQ_CONTROL_STANDARD_QUEUE"),
                controlPremiumQueue = requireEnv("ITX_RABBITMQ_CONTROL_PREMIUM_QUEUE"),
                computeStandardQueue = requireEnv("ITX_RABBITMQ_COMPUTE_STANDARD_QUEUE"),
                computePremiumQueue = requireEnv("ITX_RABBITMQ_COMPUTE_PREMIUM_QUEUE"),
            )
        }
    }

    override fun createControlStandardQueue(): MessageQueue = RabbitMessageQueue(connection, controlStandardQueue, maxConcurrency)

    override fun createControlPremiumQueue(): MessageQueue = RabbitMessageQueue(connection, controlPremiumQueue, maxConcurrency)

    override fun createComputeStandardQueue(): MessageQueue = RabbitMessageQueue(connection, computeStandardQueue, maxConcurrency)

    override fun createComputePremiumQueue(): MessageQueue = RabbitMessageQueue(connection, computePremiumQueue, maxConcurrency)
}
