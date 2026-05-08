package io.github.chehsunliu.itx.contract.queue.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record PostCreatedMessageBody(
    @JsonProperty("type") String type,
    @JsonProperty("postId") long postId,
    @JsonProperty("authorId") UUID authorId)
    implements MessageBody {

  public static final String TYPE = "post.created";

  public static PostCreatedMessageBody of(long postId, UUID authorId) {
    return new PostCreatedMessageBody(TYPE, postId, authorId);
  }
}
