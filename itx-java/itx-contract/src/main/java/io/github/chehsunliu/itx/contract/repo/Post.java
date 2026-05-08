package io.github.chehsunliu.itx.contract.repo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Post {
  long id;
  UUID authorId;
  String title;
  String body;
  List<String> tags;
  OffsetDateTime createdAt;
}
