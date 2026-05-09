import { NotFoundError, PostRepo } from "../../../../types.js";

export type ExecuteParams = {
  id: number;
  userId: string;
};

export class DeletePostUseCase {
  constructor(private readonly postRepo: PostRepo) {}

  async execute(params: ExecuteParams): Promise<boolean> {
    return this.postRepo
      .delete({ id: params.id, authorId: params.userId })
      .then(
        () => true,
        (err: unknown) => {
          if (err instanceof NotFoundError) {
            return false;
          }
          throw err;
        },
      );
  }
}
