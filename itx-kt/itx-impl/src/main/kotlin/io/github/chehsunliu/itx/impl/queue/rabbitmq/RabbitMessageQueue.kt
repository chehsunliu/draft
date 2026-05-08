package io.github.chehsunliu.itx.impl.queue.rabbitmq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.MessageProperties
import io.github.chehsunliu.itx.contract.queue.MessageHandler
import io.github.chehsunliu.itx.contract.queue.MessageQueue
import io.github.chehsunliu.itx.contract.queue.QueueException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.UUID

class RabbitMessageQueue(
    private val connection: Connection,
    private val queueName: String,
    private val maxConcurrency: Int,
) : MessageQueue {
    private val log = LoggerFactory.getLogger(RabbitMessageQueue::class.java)
    private val consumerTag = "itx-${UUID.randomUUID()}"

    /**
     * Per RabbitMQ best practice the publish channel is separate from the consume channel —
     * channels aren't safe for mixed concurrent reads and writes.
     */
    @Volatile private var publishChannel: Channel? = null

    @Volatile private var consumeChannel: Channel? = null

    private val publishLock = Any()

    override suspend fun publish(body: String) {
        try {
            withContext(Dispatchers.IO) {
                val ch =
                    synchronized(publishLock) {
                        publishChannel ?: connection.createChannel().also { publishChannel = it }
                    }
                ch.basicPublish(
                    "", // default exchange — routing_key acts as queue name
                    queueName,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    body.toByteArray(StandardCharsets.UTF_8),
                )
            }
        } catch (e: IOException) {
            throw QueueException(e.message ?: "publish failed", e)
        }
    }

    override suspend fun receive(
        handler: MessageHandler,
        cancel: MutableStateFlow<Boolean>,
    ) {
        val channel =
            try {
                withContext(Dispatchers.IO) {
                    val ch =
                        consumeChannel
                            ?: connection.createChannel().also { ch ->
                                val prefetch = minOf(maxConcurrency, 0xFFFF)
                                ch.basicQos(prefetch)
                                consumeChannel = ch
                            }
                    ch
                }
            } catch (e: IOException) {
                throw QueueException(e.message ?: "channel open failed", e)
            }

        val permits = Semaphore(maxConcurrency)
        val jobs = mutableListOf<Job>()

        try {
            coroutineScope {
                val scope = this
                channel.basicConsume(
                    queueName,
                    // autoAck =
                    false,
                    consumerTag,
                    object : DefaultConsumer(channel) {
                        override fun handleDelivery(
                            tag: String,
                            envelope: Envelope,
                            properties: AMQP.BasicProperties,
                            body: ByteArray,
                        ) {
                            if (cancel.value) {
                                runCatching { channel.basicReject(envelope.deliveryTag, true) }
                                    .onFailure { log.warn("requeue on cancel failed", it) }
                                return
                            }
                            runBlocking { permits.acquire() }
                            val job =
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val text = String(body, StandardCharsets.UTF_8)
                                        try {
                                            handler.handle(text)
                                            try {
                                                channel.basicAck(envelope.deliveryTag, false)
                                            } catch (io: IOException) {
                                                log.error("failed to ack message after success", io)
                                            }
                                        } catch (e: Throwable) {
                                            log.warn("handler failed; rejecting to DLQ", e)
                                            try {
                                                channel.basicReject(envelope.deliveryTag, false)
                                            } catch (io: IOException) {
                                                log.error("failed to reject after handler failure", io)
                                            }
                                        }
                                    } finally {
                                        permits.release()
                                    }
                                }
                            jobs.add(job)
                        }
                    },
                )

                while (!cancel.value) delay(200)

                runCatching { channel.basicCancel(consumerTag) }
                    .onFailure { log.warn("basicCancel failed", it) }

                log.info("draining in-flight handlers for queue {}", queueName)
                jobs.forEach { it.join() }
            }
        } catch (e: IOException) {
            throw QueueException(e.message ?: "receive failed", e)
        }
    }
}
