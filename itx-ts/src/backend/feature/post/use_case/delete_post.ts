import { PostRepo } from "../../../../types.js";

export type ExecuteParams = {
  id: number;
  userId: string;
};

export class DeletePostUseCase {
  constructor(private readonly postRepo: PostRepo) {}

  async execute(params: ExecuteParams): Promise<void> {
    await this.postRepo.delete({ id: params.id, authorId: params.userId });
  }
}
