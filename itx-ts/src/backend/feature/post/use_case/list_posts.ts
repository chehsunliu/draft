import { PostDto, PostRepo } from "../../../../types.js";
import { toPostDto } from "../../../../util.js";

export type ExecuteParams = {
  userId: string;
  limit: number;
  offset: number;
};

export type ExecuteOutput = {
  items: PostDto[];
};

export class ListPostsUseCase {
  constructor(private readonly postRepo: PostRepo) {}

  async execute(params: ExecuteParams): Promise<ExecuteOutput> {
    const posts = await this.postRepo.list({
      authorId: params.userId,
      limit: params.limit,
      offset: params.offset,
    });
    return { items: posts.map(toPostDto) };
  }
}
