package io.github.chehsunliu.itx.backend.feature.post.usecase;

import io.github.chehsunliu.itx.backend.feature.post.PostDto;
import io.github.chehsunliu.itx.contract.repo.Post;
import io.github.chehsunliu.itx.contract.repo.PostRepo;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class UpdatePostUseCase {
  public record ExecuteParams(long id, UUID userId, String title, String body, List<String> tags) {}

  private final PostRepo postRepo;

  public PostDto execute(ExecuteParams params) {
    Post post =
        postRepo.update(
            new PostRepo.UpdateParams(
                params.id, params.userId, params.title, params.body, params.tags));
    return PostDto.fromPost(post);
  }
}
