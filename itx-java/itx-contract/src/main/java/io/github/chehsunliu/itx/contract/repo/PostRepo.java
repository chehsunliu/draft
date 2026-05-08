package io.github.chehsunliu.itx.contract.repo;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

public interface PostRepo {

  List<Post> list(ListParams params);

  /** Throws {@link RepoNotFoundException} if no post with {@code params.id} exists. */
  Post get(GetParams params);

  Post create(CreateParams params);

  /**
   * Updates a post owned by {@code params.authorId}. Throws {@link RepoNotFoundException} if the
   * post does not exist or is not owned by the caller.
   */
  Post update(UpdateParams params);

  /**
   * Deletes a post owned by {@code params.authorId}. Throws {@link RepoNotFoundException} if the
   * post does not exist or is not owned by the caller.
   */
  void delete(DeleteParams params);

  @Value
  @Builder
  class ListParams {
    UUID authorId;
    int limit;
    int offset;
  }

  @Value
  @Builder
  class GetParams {
    long id;
  }

  @Value
  @Builder
  class CreateParams {
    UUID authorId;
    String title;
    String body;
    List<String> tags;
  }

  @Value
  @Builder
  class UpdateParams {
    long id;
    UUID authorId;
    String title;
    String body;
    List<String> tags;
  }

  @Value
  @Builder
  class DeleteParams {
    long id;
    UUID authorId;
  }
}
