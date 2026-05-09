import { repoFactoryFromEnv } from "../db/index.js";
import { queueFactoryFromEnv } from "../queue/index.js";
import {
  MessageQueue,
  PostRepo,
  QueueFactory,
  RepoFactory,
  SubscriptionRepo,
  UserRepo,
} from "../types.js";

export type AppState = {
  repoFactory: RepoFactory;
  queueFactory: QueueFactory;
  postRepo: PostRepo;
  userRepo: UserRepo;
  subscriptionRepo: SubscriptionRepo;
  controlStandardQueue: MessageQueue;
  close(): Promise<void>;
};

export async function appStateFromEnv(): Promise<AppState> {
  const repoFactory = repoFactoryFromEnv();
  const queueFactory = await queueFactoryFromEnv();
  return {
    repoFactory,
    queueFactory,
    postRepo: repoFactory.postRepo(),
    userRepo: repoFactory.userRepo(),
    subscriptionRepo: repoFactory.subscriptionRepo(),
    controlStandardQueue: queueFactory.controlStandardQueue(),
    async close() {
      await Promise.all([repoFactory.close(), queueFactory.close()]);
    },
  };
}
