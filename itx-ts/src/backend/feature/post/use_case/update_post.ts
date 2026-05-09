import { NotFoundError, PostDto, PostRepo } from "../../../../types.js";
import { toPostDto } from "../../../../util.js";

export type ExecuteParams = {
  id: number;
  userId: string;
  title?: string;
  body?: string;
  tags?: string[];
};

export class UpdatePostUseCase {
  constructor(private readonly postRepo: PostRepo) {}

  async execute(params: ExecuteParams): Promise<PostDto | null> {
    const post = await this.postRepo
      .update({
        id: params.id,
        authorId: params.userId,
        title: params.title,
        body: params.body,
        tags: params.tags,
      })
      .catch((err: unknown) => {
        if (err instanceof NotFoundError) {
          return null;
        }
        throw err;
      });
    return post ? toPostDto(post) : null;
  }
}
