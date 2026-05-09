import { NotFoundError, PostDto, PostRepo } from "../../../../types.js";
import { toPostDto } from "../../../../util.js";

export type ExecuteParams = {
  id: number;
  userId: string;
};

export class GetPostUseCase {
  constructor(private readonly postRepo: PostRepo) {}

  async execute(params: ExecuteParams): Promise<PostDto | null> {
    const post = await this.postRepo.get(params.id).catch((err: unknown) => {
      if (err instanceof NotFoundError) {
        return null;
      }
      throw err;
    });
    if (!post || post.authorId !== params.userId) {
      return null;
    }
    return toPostDto(post);
  }
}
