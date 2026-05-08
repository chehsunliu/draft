package io.github.chehsunliu.itx.worker.run

import io.github.chehsunliu.itx.contract.queue.MessageHandler
import io.github.chehsunliu.itx.contract.queue.MessageQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import org.slf4j.LoggerFactory
import sun.misc.Signal

private val log = LoggerFactory.getLogger("io.github.chehsunliu.itx.worker.QueueRunner")

/**
 * Runs queue listeners in parallel, dispatching every message to [handler]. Returns when
 * SIGTERM/SIGINT is received and all listeners have drained.
 */
suspend fun runQueueLoops(
    queues: List<MessageQueue>,
    handler: MessageHandler,
    registerSignals: Boolean = false,
) {
    val cancel = MutableStateFlow(false)

    if (registerSignals) {
        Signal.handle(Signal("INT")) { cancel.value = true }
        Signal.handle(Signal("TERM")) { cancel.value = true }
    }

    coroutineScope {
        val jobs =
            queues.map { q ->
                async(Dispatchers.IO) {
                    try {
                        q.receive(handler, cancel)
                        log.info("queue listener returned cleanly")
                    } catch (e: Throwable) {
                        log.error("queue listener errored", e)
                    }
                }
            }
        jobs.awaitAll()
    }
}
