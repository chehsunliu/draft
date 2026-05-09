import { WorkerState } from "./state.js";

type PostCreatedMessage = {
  type: "post.created";
  postId: number;
  authorId: string;
};

export class ControlDispatcher {
  constructor(private readonly state: WorkerState) {}

  async handle(body: string): Promise<void> {
    const envelope = JSON.parse(body) as { type?: string };
    switch (envelope.type) {
      case "post.created":
        await this.handlePostCreated(envelope as PostCreatedMessage);
        break;
      default:
        throw new Error(`unknown message type: ${envelope.type}`);
    }
  }

  private async handlePostCreated(message: PostCreatedMessage): Promise<void> {
    const post = await this.state.postRepo.get(message.postId);
    const author = await this.state.userRepo.get(message.authorId);
    const subscribers = await this.state.subscriptionRepo.listSubscribers(
      message.authorId,
    );

    for (const subscriber of subscribers) {
      await this.state.emailClient.send({
        to: subscriber.email,
        subject: `${author.email} just published a new post`,
        body: `Check out the new post: ${post.title}`,
      });
    }

    await this.state.postRepo.markNotified(message.postId);
  }
}
