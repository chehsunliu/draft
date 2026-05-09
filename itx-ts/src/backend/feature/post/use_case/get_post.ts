import { NotFoundError, PostDto, PostRepo } from "../../../../types.js";
import { toPostDto } from "../../../../util.js";

export type ExecuteParams = {
  id: number;
  userId: string;
};

export class GetPostUseCase {
  constructor(private readonly postRepo: PostRepo) {}

  async execute(params: ExecuteParams): Promise<PostDto> {
    const post = await this.postRepo.get(params.id);
    if (post.authorId !== params.userId) {
      throw new NotFoundError();
    }
    return toPostDto(post);
  }
}
