import { SubscriptionRepo, UserRepo } from "../../../../types.js";

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

  async execute(params: ExecuteParams): Promise<void> {
    await this.userRepo.get(params.authorId);
    await this.userRepo.upsert({
      id: params.subscriberId,
      email: params.subscriberEmail,
    });
    await this.subscriptionRepo.subscribe({
      subscriberId: params.subscriberId,
      authorId: params.authorId,
    });
  }
}
