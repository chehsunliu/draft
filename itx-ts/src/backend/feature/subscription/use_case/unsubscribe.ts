import {
  NotFoundError,
  SubscriptionRepo,
  UserRepo,
} from "../../../../types.js";

export type ExecuteParams = {
  subscriberId: string;
  authorId: string;
};

export class UnsubscribeUseCase {
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
    await this.subscriptionRepo.unsubscribe(params);
    return true;
  }
}
