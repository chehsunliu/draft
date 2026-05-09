import { SubscriptionRepo, User, UserRepo } from "../../../../types.js";

export type ExecuteParams = {
  subscriberId: string;
};

export type ExecuteOutput = {
  items: User[];
};

export class ListSubscriptionsUseCase {
  constructor(
    private readonly userRepo: UserRepo,
    private readonly subscriptionRepo: SubscriptionRepo,
  ) {}

  async execute(params: ExecuteParams): Promise<ExecuteOutput> {
    await this.userRepo.get(params.subscriberId);
    const authors = await this.subscriptionRepo.listAuthors(
      params.subscriberId,
    );
    return { items: authors };
  }
}
