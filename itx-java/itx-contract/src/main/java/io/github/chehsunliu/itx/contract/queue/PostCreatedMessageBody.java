package io.github.chehsunliu.itx.contract.queue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record PostCreatedMessageBody(String type, long postId, String authorId)
    implements MessageBody {
  public static final String TYPE = "post.created";

  @JsonCreator
  public PostCreatedMessageBody(
      @JsonProperty("type") String type,
      @JsonProperty("postId") long postId,
      @JsonProperty("authorId") String authorId) {
    this.type = type == null ? TYPE : type;
    this.postId = postId;
    this.authorId = authorId;
  }

  public static PostCreatedMessageBody of(long postId, UUID authorId) {
    return new PostCreatedMessageBody(TYPE, postId, authorId.toString());
  }
}
