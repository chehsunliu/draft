package io.github.chehsunliu.itx.worker.compute

import io.github.chehsunliu.itx.contract.queue.MessageHandler
import org.slf4j.LoggerFactory

class ComputeDispatcher : MessageHandler {
    private val log = LoggerFactory.getLogger(ComputeDispatcher::class.java)

    override suspend fun handle(body: String) {
        // No compute-plane message types yet — just log and ack so the queue stays drained.
        log.info("compute message received (no handler yet) body={}", body)
    }
}
