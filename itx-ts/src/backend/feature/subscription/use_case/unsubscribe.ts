import { SubscriptionRepo, UserRepo } from "../../../../types.js";

export type ExecuteParams = {
  subscriberId: string;
  authorId: string;
};

export class UnsubscribeUseCase {
  constructor(
    private readonly userRepo: UserRepo,
    private readonly subscriptionRepo: SubscriptionRepo,
  ) {}

  async execute(params: ExecuteParams): Promise<void> {
    await this.userRepo.get(params.authorId);
    await this.subscriptionRepo.unsubscribe(params);
  }
}
