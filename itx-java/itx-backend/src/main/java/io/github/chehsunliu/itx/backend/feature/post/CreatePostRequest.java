package io.github.chehsunliu.itx.backend.feature.post;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CreatePostRequest(String title, String body, List<String> tags) {
  @JsonCreator
  public CreatePostRequest(
      @JsonProperty("title") String title,
      @JsonProperty("body") String body,
      @JsonProperty("tags") List<String> tags) {
    this.title = title;
    this.body = body;
    this.tags = tags == null ? List.of() : tags;
  }
}
