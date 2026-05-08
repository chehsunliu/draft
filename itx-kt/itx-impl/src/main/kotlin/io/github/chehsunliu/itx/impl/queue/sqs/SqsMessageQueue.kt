package io.github.chehsunliu.itx.impl.queue.sqs

import io.github.chehsunliu.itx.contract.queue.MessageHandler
import io.github.chehsunliu.itx.contract.queue.MessageQueue
import io.github.chehsunliu.itx.contract.queue.QueueException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

class SqsMessageQueue(
    private val client: SqsClient,
    private val queueUrl: String,
    private val maxConcurrency: Int,
) : MessageQueue {
    private val log = LoggerFactory.getLogger(SqsMessageQueue::class.java)

    override suspend fun publish(body: String) {
        try {
            withContext(Dispatchers.IO) {
                client.sendMessage { it.queueUrl(queueUrl).messageBody(body) }
            }
        } catch (e: RuntimeException) {
            throw QueueException(e.message ?: "publish failed", e)
        }
    }

    override suspend fun receive(
        handler: MessageHandler,
        cancel: MutableStateFlow<Boolean>,
    ) {
        val batch = minOf(10, maxOf(1, maxConcurrency))
        val permits = Semaphore(maxConcurrency)
        val jobs = mutableListOf<Job>()

        try {
            coroutineScope {
                while (!cancel.value) {
                    val resp =
                        withContext(Dispatchers.IO) {
                            client.receiveMessage(
                                ReceiveMessageRequest
                                    .builder()
                                    .queueUrl(queueUrl)
                                    .maxNumberOfMessages(batch)
                                    .waitTimeSeconds(20)
                                    .build(),
                            )
                        }
                    for (msg in resp.messages()) {
                        if (cancel.value) break
                        permits.acquire()
                        val job =
                            launch(Dispatchers.IO) {
                                try {
                                    handler.handle(msg.body())
                                    client.deleteMessage {
                                        it.queueUrl(queueUrl).receiptHandle(msg.receiptHandle())
                                    }
                                } catch (e: Throwable) {
                                    // Failure: skip delete. The message becomes visible again
                                    // after the visibility timeout, eventually landing in the
                                    // DLQ once it exceeds maxReceiveCount.
                                    log.warn("handler failed; leaving message for retry/DLQ", e)
                                } finally {
                                    permits.release()
                                }
                            }
                        jobs.add(job)
                    }
                }
                log.info("draining in-flight handlers for queue {}", queueUrl)
                jobs.forEach { it.join() }
            }
        } catch (e: RuntimeException) {
            throw QueueException(e.message ?: "receive failed", e)
        }
    }
}
