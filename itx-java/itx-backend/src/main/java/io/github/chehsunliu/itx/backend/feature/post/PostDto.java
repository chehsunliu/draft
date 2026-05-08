package io.github.chehsunliu.itx.backend.feature.post;

import io.github.chehsunliu.itx.contract.repo.Post;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PostDto(
    long id,
    UUID authorId,
    String title,
    String body,
    List<String> tags,
    OffsetDateTime createdAt) {
  public static PostDto fromPost(Post p) {
    return new PostDto(p.id(), p.authorId(), p.title(), p.body(), p.tags(), p.createdAt());
  }
}
