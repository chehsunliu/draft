package io.github.chehsunliu.itx.contract.repo;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class User {
  UUID id;
  String email;
}
