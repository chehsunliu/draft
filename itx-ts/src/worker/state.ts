import { repoFactoryFromEnv } from "../db/index.js";
import { HttpEmailClient } from "../email/http.js";
import { queueFactoryFromEnv } from "../queue/index.js";
import {
  EmailClient,
  MessageQueue,
  PostRepo,
  QueueFactory,
  RepoFactory,
  SubscriptionRepo,
  UserRepo,
} from "../types.js";

export type WorkerState = {
  repoFactory: RepoFactory;
  queueFactory: QueueFactory;
  postRepo: PostRepo;
  userRepo: UserRepo;
  subscriptionRepo: SubscriptionRepo;
  controlStandardQueue: MessageQueue;
  controlPremiumQueue: MessageQueue;
  computeStandardQueue: MessageQueue;
  computePremiumQueue: MessageQueue;
  emailClient: EmailClient;
  close(): Promise<void>;
};

export async function workerStateFromEnv(): Promise<WorkerState> {
  const repoFactory = repoFactoryFromEnv();
  const queueFactory = await queueFactoryFromEnv();
  return {
    repoFactory,
    queueFactory,
    postRepo: repoFactory.postRepo(),
    userRepo: repoFactory.userRepo(),
    subscriptionRepo: repoFactory.subscriptionRepo(),
    controlStandardQueue: queueFactory.controlStandardQueue(),
    controlPremiumQueue: queueFactory.controlPremiumQueue(),
    computeStandardQueue: queueFactory.computeStandardQueue(),
    computePremiumQueue: queueFactory.computePremiumQueue(),
    emailClient: new HttpEmailClient(),
    async close() {
      await Promise.all([repoFactory.close(), queueFactory.close()]);
    },
  };
}
