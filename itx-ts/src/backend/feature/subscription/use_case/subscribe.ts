import {
  NotFoundError,
  SubscriptionRepo,
  UserRepo,
} from "../../../../types.js";

export type ExecuteParams = {
  subscriberId: string;
  subscriberEmail: string;
  authorId: string;
};

export class SubscribeUseCase {
  constructor(
    private readonly userRepo: UserRepo,
    private readonly subscriptionRepo: SubscriptionRepo,
  ) {}

  async execute(params: ExecuteParams): Promise<boolean> {
    const author = await this.userRepo.get(params.authorId).catch((err) => {
      if (err instanceof NotFoundError) {
        return null;
      }
      throw err;
    });
    if (!author) {
      return false;
    }
    await this.userRepo.upsert({
      id: params.subscriberId,
      email: params.subscriberEmail,
    });
    await this.subscriptionRepo.subscribe({
      subscriberId: params.subscriberId,
      authorId: params.authorId,
    });
    return true;
  }
}
