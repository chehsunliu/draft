import { User, UserRepo } from "../../../../types.js";

export type ExecuteParams = {
  userId: string;
  userEmail: string;
};

export class GetMeUseCase {
  constructor(private readonly userRepo: UserRepo) {}

  async execute(params: ExecuteParams): Promise<User> {
    return this.userRepo.upsert({
      id: params.userId,
      email: params.userEmail,
    });
  }
}
