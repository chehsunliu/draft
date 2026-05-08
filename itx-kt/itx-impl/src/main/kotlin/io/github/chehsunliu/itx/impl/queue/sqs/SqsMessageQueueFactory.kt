package io.github.chehsunliu.itx.impl.queue.sqs

import io.github.chehsunliu.itx.contract.queue.MessageQueue
import io.github.chehsunliu.itx.contract.queue.MessageQueueFactory
import io.github.chehsunliu.itx.impl.envInt
import io.github.chehsunliu.itx.impl.requireEnv
import software.amazon.awssdk.services.sqs.SqsClient
import java.net.URI

class SqsMessageQueueFactory(
    private val client: SqsClient,
    private val maxConcurrency: Int,
    private val controlStandardQueueUrl: String,
    private val controlPremiumQueueUrl: String,
    private val computeStandardQueueUrl: String,
    private val computePremiumQueueUrl: String,
) : MessageQueueFactory {
    companion object {
        fun fromEnv(): SqsMessageQueueFactory {
            val endpoint = System.getenv("ITX_SQS_LOCAL_ENDPOINT_URL")
            val builder = SqsClient.builder()
            if (!endpoint.isNullOrBlank()) builder.endpointOverride(URI.create(endpoint))
            return SqsMessageQueueFactory(
                client = builder.build(),
                maxConcurrency = envInt("ITX_SQS_MAX_CONCURRENCY", 100),
                controlStandardQueueUrl = requireEnv("ITX_SQS_CONTROL_STANDARD_QUEUE_URL"),
                controlPremiumQueueUrl = requireEnv("ITX_SQS_CONTROL_PREMIUM_QUEUE_URL"),
                computeStandardQueueUrl = requireEnv("ITX_SQS_COMPUTE_STANDARD_QUEUE_URL"),
                computePremiumQueueUrl = requireEnv("ITX_SQS_COMPUTE_PREMIUM_QUEUE_URL"),
            )
        }
    }

    override fun createControlStandardQueue(): MessageQueue = SqsMessageQueue(client, controlStandardQueueUrl, maxConcurrency)

    override fun createControlPremiumQueue(): MessageQueue = SqsMessageQueue(client, controlPremiumQueueUrl, maxConcurrency)

    override fun createComputeStandardQueue(): MessageQueue = SqsMessageQueue(client, computeStandardQueueUrl, maxConcurrency)

    override fun createComputePremiumQueue(): MessageQueue = SqsMessageQueue(client, computePremiumQueueUrl, maxConcurrency)
}
