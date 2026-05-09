import { MessageQueue } from "../types.js";

export async function runQueues(
  queues: MessageQueue[],
  handler: (body: string) => Promise<void>,
): Promise<void> {
  const controller = new AbortController();
  const shutdown = () => controller.abort();
  process.once("SIGINT", shutdown);
  process.once("SIGTERM", shutdown);

  await Promise.race([
    Promise.all(
      queues.map((queue) => queue.receive(handler, controller.signal)),
    ),
    new Promise<void>((resolve) =>
      controller.signal.addEventListener("abort", () => resolve(), {
        once: true,
      }),
    ),
  ]);
}
