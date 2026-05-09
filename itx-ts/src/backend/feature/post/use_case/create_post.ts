import { MessageQueue, PostDto, PostRepo } from "../../../../types.js";
import { toPostDto } from "../../../../util.js";

export type ExecuteParams = {
  userId: string;
  title: string;
  body: string;
  tags: string[];
};

export class CreatePostUseCase {
  constructor(
    private readonly postRepo: PostRepo,
    private readonly controlStandardQueue: MessageQueue,
  ) {}

  async execute(params: ExecuteParams): Promise<PostDto> {
    const post = await this.postRepo.create({
      authorId: params.userId,
      title: params.title,
      body: params.body,
      tags: params.tags,
    });
    await this.controlStandardQueue.publish(
      JSON.stringify({
        type: "post.created",
        postId: post.id,
        authorId: post.authorId,
      }),
    );
    return toPostDto(post);
  }
}
