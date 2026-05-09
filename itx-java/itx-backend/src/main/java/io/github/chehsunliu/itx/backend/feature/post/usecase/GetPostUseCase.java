package io.github.chehsunliu.itx.backend.feature.post.usecase;

import io.github.chehsunliu.itx.backend.error.BackendException;
import io.github.chehsunliu.itx.backend.feature.post.PostDto;
import io.github.chehsunliu.itx.contract.repo.Post;
import io.github.chehsunliu.itx.contract.repo.PostRepo;
import java.util.UUID;

public final class GetPostUseCase {
  public record ExecuteParams(long id, UUID userId) {}

  private final PostRepo postRepo;

  public GetPostUseCase(PostRepo postRepo) {
    this.postRepo = postRepo;
  }

  public PostDto execute(ExecuteParams params) {
    Post post = postRepo.get(new PostRepo.GetParams(params.id));
    if (!post.authorId().equals(params.userId)) throw BackendException.notFound();
    return PostDto.fromPost(post);
  }
}
