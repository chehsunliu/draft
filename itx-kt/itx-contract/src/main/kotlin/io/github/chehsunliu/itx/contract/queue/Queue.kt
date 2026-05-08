package io.github.chehsunliu.itx.contract.queue

import kotlinx.coroutines.flow.MutableStateFlow

class QueueException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

fun interface MessageHandler {
    /**
     * Processes a single message body. Returning normally causes the queue to ack/delete; throwing
     * causes the queue to nack/reject so the broker can route the message to its DLQ.
     */
    suspend fun handle(body: String)
}

interface MessageQueue {
    /** Publish a message to this queue. */
    suspend fun publish(body: String)

    /**
     * Run the consumer loop, dispatching each message to [handler]. When [cancel] flips to `true`
     * the impl stops pulling new messages and waits for in-flight handlers to finish (graceful
     * drain) before returning.
     */
    suspend fun receive(
        handler: MessageHandler,
        cancel: MutableStateFlow<Boolean>,
    )
}

interface MessageQueueFactory {
    fun createControlStandardQueue(): MessageQueue

    fun createControlPremiumQueue(): MessageQueue

    fun createComputeStandardQueue(): MessageQueue

    fun createComputePremiumQueue(): MessageQueue
}
