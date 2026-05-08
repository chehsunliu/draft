package io.github.chehsunliu.itx.contract.queue.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({@JsonSubTypes.Type(value = PostCreatedMessageBody.class, name = "post.created")})
public interface MessageBody {}
