import {
  NotFoundError,
  SubscriptionRepo,
  User,
  UserRepo,
} from "../../../../types.js";

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

  async execute(params: ExecuteParams): Promise<ExecuteOutput | null> {
    const user = await this.userRepo
      .get(params.subscriberId)
      .catch((err: unknown) => {
        if (err instanceof NotFoundError) {
          return null;
        }
        throw err;
      });
    if (!user) {
      return null;
    }
    const authors = await this.subscriptionRepo.listAuthors(
      params.subscriberId,
    );
    return { items: authors };
  }
}
