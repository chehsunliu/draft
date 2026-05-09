import { QueueFactory } from "../types.js";
import { RabbitQueueFactory } from "./rabbitmq.js";
import { SqsQueueFactory } from "./sqs.js";

export async function queueFactoryFromEnv(): Promise<QueueFactory> {
  const provider = process.env.ITX_QUEUE_PROVIDER || "sqs";
  switch (provider) {
    case "sqs":
      return new SqsQueueFactory();
    case "rabbitmq":
      return RabbitQueueFactory.fromEnv();
    default:
      throw new Error(`unknown ITX_QUEUE_PROVIDER: ${provider}`);
  }
}
