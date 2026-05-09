import amqp from "amqplib";
import { randomUUID } from "node:crypto";
import { MessageQueue, QueueFactory } from "../types.js";
import { env, parsePositiveInt } from "../util.js";

class RabbitMessageQueue implements MessageQueue {
  constructor(
    private readonly conn: amqp.ChannelModel,
    private readonly queueName: string,
    private readonly maxConcurrency: number,
  ) {}

  async publish(body: string): Promise<void> {
    const ch = await this.conn.createChannel();
    try {
      ch.sendToQueue(this.queueName, Buffer.from(body), {
        contentType: "application/json",
        persistent: true,
      });
    } finally {
      await ch.close();
    }
  }

  async receive(
    handler: (body: string) => Promise<void>,
    signal: AbortSignal,
  ): Promise<void> {
    const ch = await this.conn.createChannel();
    await ch.prefetch(Math.max(1, Math.min(65535, this.maxConcurrency)));
    const consumerTag = `itx-${randomUUID()}`;
    signal.addEventListener(
      "abort",
      () => void ch.cancel(consumerTag).catch(() => undefined),
      { once: true },
    );

    await ch.consume(
      this.queueName,
      (message) => {
        if (!message) {
          return;
        }
        void (async () => {
          try {
            await handler(message.content.toString("utf8"));
            ch.ack(message);
          } catch (err) {
            console.warn("handler failed; rejecting to DLQ", err);
            ch.reject(message, false);
          }
        })();
      },
      { noAck: false, consumerTag },
    );

    await new Promise<void>((resolve) => {
      if (signal.aborted) {
        resolve();
        return;
      }
      signal.addEventListener("abort", () => resolve(), { once: true });
    });
    await ch.close().catch(() => undefined);
  }

  async close(): Promise<void> {}
}

export class RabbitQueueFactory implements QueueFactory {
  private readonly maxConcurrency: number;

  private constructor(private readonly conn: amqp.ChannelModel) {
    this.maxConcurrency = parsePositiveInt(
      process.env.ITX_RABBITMQ_MAX_CONCURRENCY,
      100,
    );
  }

  static async fromEnv(): Promise<RabbitQueueFactory> {
    const url = `amqp://${env("ITX_RABBITMQ_USER")}:${env("ITX_RABBITMQ_PASSWORD")}@${env("ITX_RABBITMQ_HOST")}:${env("ITX_RABBITMQ_PORT")}/%2F`;
    return new RabbitQueueFactory(await amqp.connect(url));
  }

  controlStandardQueue(): MessageQueue {
    return new RabbitMessageQueue(
      this.conn,
      env("ITX_RABBITMQ_CONTROL_STANDARD_QUEUE"),
      this.maxConcurrency,
    );
  }

  controlPremiumQueue(): MessageQueue {
    return new RabbitMessageQueue(
      this.conn,
      env("ITX_RABBITMQ_CONTROL_PREMIUM_QUEUE"),
      this.maxConcurrency,
    );
  }

  computeStandardQueue(): MessageQueue {
    return new RabbitMessageQueue(
      this.conn,
      env("ITX_RABBITMQ_COMPUTE_STANDARD_QUEUE"),
      this.maxConcurrency,
    );
  }

  computePremiumQueue(): MessageQueue {
    return new RabbitMessageQueue(
      this.conn,
      env("ITX_RABBITMQ_COMPUTE_PREMIUM_QUEUE"),
      this.maxConcurrency,
    );
  }

  async close(): Promise<void> {
    await this.conn.close();
  }
}
