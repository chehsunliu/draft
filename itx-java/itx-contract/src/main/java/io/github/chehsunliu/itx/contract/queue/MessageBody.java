package io.github.chehsunliu.itx.contract.queue;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = PostCreatedMessageBody.class, name = "post.created")})
public sealed interface MessageBody permits PostCreatedMessageBody {
  String type();
}
