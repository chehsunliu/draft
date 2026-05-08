package io.github.chehsunliu.itx.backend.feature.post;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PostDto {
  long id;
  UUID authorId;
  String title;
  String body;
  List<String> tags;

  @JsonFormat(shape = JsonFormat.Shape.STRING)
  OffsetDateTime createdAt;
}
