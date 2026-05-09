import {
  DeleteMessageCommand,
  ReceiveMessageCommand,
  SendMessageCommand,
  SQSClient,
} from "@aws-sdk/client-sqs";
import { MessageQueue, QueueFactory } from "../types.js";
import { env, parsePositiveInt } from "../util.js";

class SqsMessageQueue implements MessageQueue {
  constructor(
    private readonly client: SQSClient,
    private readonly queueUrl: string,
    private readonly maxConcurrency: number,
  ) {}

  async publish(body: string): Promise<void> {
    await this.client.send(
      new SendMessageCommand({ QueueUrl: this.queueUrl, MessageBody: body }),
    );
  }

  async receive(
    handler: (body: string) => Promise<void>,
    signal: AbortSignal,
  ): Promise<void> {
    const batch = Math.max(1, Math.min(10, this.maxConcurrency));
    while (!signal.aborted) {
      const response = await this.client
        .send(
          new ReceiveMessageCommand({
            QueueUrl: this.queueUrl,
            MaxNumberOfMessages: batch,
            WaitTimeSeconds: 1,
          }),
          { abortSignal: signal },
        )
        .catch((err: unknown) => {
          if (signal.aborted) {
            return null;
          }
          throw err;
        });
      if (!response) {
        break;
      }
      for (const message of response.Messages ?? []) {
        if (!message.Body || !message.ReceiptHandle) {
          continue;
        }
        try {
          await handler(message.Body);
          await this.client.send(
            new DeleteMessageCommand({
              QueueUrl: this.queueUrl,
              ReceiptHandle: message.ReceiptHandle,
            }),
          );
        } catch (err) {
          console.warn("handler failed; leaving message for retry/DLQ", err);
        }
      }
    }
  }

  async close(): Promise<void> {
    this.client.destroy();
  }
}

export class SqsQueueFactory implements QueueFactory {
  private readonly client: SQSClient;
  private readonly maxConcurrency: number;

  constructor() {
    const endpoint = env("ITX_SQS_LOCAL_ENDPOINT_URL");
    this.client = new SQSClient({
      region: process.env.AWS_REGION || "us-east-1",
      endpoint: endpoint || undefined,
    });
    this.maxConcurrency = parsePositiveInt(
      process.env.ITX_SQS_MAX_CONCURRENCY,
      100,
    );
  }

  controlStandardQueue(): MessageQueue {
    return new SqsMessageQueue(
      this.client,
      env("ITX_SQS_CONTROL_STANDARD_QUEUE_URL"),
      this.maxConcurrency,
    );
  }

  controlPremiumQueue(): MessageQueue {
    return new SqsMessageQueue(
      this.client,
      env("ITX_SQS_CONTROL_PREMIUM_QUEUE_URL"),
      this.maxConcurrency,
    );
  }

  computeStandardQueue(): MessageQueue {
    return new SqsMessageQueue(
      this.client,
      env("ITX_SQS_COMPUTE_STANDARD_QUEUE_URL"),
      this.maxConcurrency,
    );
  }

  computePremiumQueue(): MessageQueue {
    return new SqsMessageQueue(
      this.client,
      env("ITX_SQS_COMPUTE_PREMIUM_QUEUE_URL"),
      this.maxConcurrency,
    );
  }

  async close(): Promise<void> {
    this.client.destroy();
  }
}
