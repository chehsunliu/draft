export type User = {
  id: string;
  email: string;
};

export type Post = {
  id: number;
  authorId: string;
  title: string;
  body: string;
  tags: string[];
  createdAt: Date;
  notifiedAt: Date | null;
};

export type PostDto = {
  id: number;
  authorId: string;
  title: string;
  body: string;
  tags: string[];
  createdAt: string;
};

export class NotFoundError extends Error {
  constructor() {
    super("not found");
  }
}

export interface PostRepo {
  list(params: {
    authorId?: string;
    limit: number;
    offset: number;
  }): Promise<Post[]>;
  get(id: number): Promise<Post>;
  create(params: {
    authorId: string;
    title: string;
    body: string;
    tags: string[];
  }): Promise<Post>;
  update(params: {
    id: number;
    authorId: string;
    title?: string;
    body?: string;
    tags?: string[];
  }): Promise<Post>;
  delete(params: { id: number; authorId: string }): Promise<void>;
  markNotified(id: number): Promise<void>;
}

export interface UserRepo {
  upsert(params: { id: string; email: string }): Promise<User>;
  get(id: string): Promise<User>;
}

export interface SubscriptionRepo {
  subscribe(params: { subscriberId: string; authorId: string }): Promise<void>;
  unsubscribe(params: {
    subscriberId: string;
    authorId: string;
  }): Promise<void>;
  listAuthors(subscriberId: string): Promise<User[]>;
  listSubscribers(authorId: string): Promise<User[]>;
}

export interface RepoFactory {
  postRepo(): PostRepo;
  userRepo(): UserRepo;
  subscriptionRepo(): SubscriptionRepo;
  close(): Promise<void>;
}

export interface MessageQueue {
  publish(body: string): Promise<void>;
  receive(
    handler: (body: string) => Promise<void>,
    signal: AbortSignal,
  ): Promise<void>;
  close(): Promise<void>;
}

export interface QueueFactory {
  controlStandardQueue(): MessageQueue;
  controlPremiumQueue(): MessageQueue;
  computeStandardQueue(): MessageQueue;
  computePremiumQueue(): MessageQueue;
  close(): Promise<void>;
}

export interface EmailClient {
  send(message: { to: string; subject: string; body: string }): Promise<void>;
}
