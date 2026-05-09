package io.github.chehsunliu.itx.impl.queue.rabbitmq;

import static io.github.chehsunliu.itx.impl.Env.envInt;
import static io.github.chehsunliu.itx.impl.Env.requireEnv;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.github.chehsunliu.itx.contract.queue.MessageQueue;
import io.github.chehsunliu.itx.contract.queue.MessageQueueFactory;
import io.github.chehsunliu.itx.contract.queue.QueueException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class RabbitMessageQueueFactory implements MessageQueueFactory {
    private final Connection connection;
    private final int maxConcurrency;
    private final String controlStandardQueue;
    private final String controlPremiumQueue;
    private final String computeStandardQueue;
    private final String computePremiumQueue;

    public static RabbitMessageQueueFactory fromEnv() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(requireEnv("ITX_RABBITMQ_HOST"));
        factory.setPort(Integer.parseInt(requireEnv("ITX_RABBITMQ_PORT")));
        factory.setUsername(requireEnv("ITX_RABBITMQ_USER"));
        factory.setPassword(requireEnv("ITX_RABBITMQ_PASSWORD"));
        Connection conn;
        try {
            conn = factory.newConnection("itx-java");
        } catch (Exception e) {
            throw new QueueException("failed to open RabbitMQ connection", e);
        }
        return new RabbitMessageQueueFactory(
                conn,
                envInt("ITX_RABBITMQ_MAX_CONCURRENCY", 100),
                requireEnv("ITX_RABBITMQ_CONTROL_STANDARD_QUEUE"),
                requireEnv("ITX_RABBITMQ_CONTROL_PREMIUM_QUEUE"),
                requireEnv("ITX_RABBITMQ_COMPUTE_STANDARD_QUEUE"),
                requireEnv("ITX_RABBITMQ_COMPUTE_PREMIUM_QUEUE"));
    }

    @Override
    public MessageQueue createControlStandardQueue() {
        return new RabbitMessageQueue(connection, controlStandardQueue, maxConcurrency);
    }

    @Override
    public MessageQueue createControlPremiumQueue() {
        return new RabbitMessageQueue(connection, controlPremiumQueue, maxConcurrency);
    }

    @Override
    public MessageQueue createComputeStandardQueue() {
        return new RabbitMessageQueue(connection, computeStandardQueue, maxConcurrency);
    }

    @Override
    public MessageQueue createComputePremiumQueue() {
        return new RabbitMessageQueue(connection, computePremiumQueue, maxConcurrency);
    }
}
