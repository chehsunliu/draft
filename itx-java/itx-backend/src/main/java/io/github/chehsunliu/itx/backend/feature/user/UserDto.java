package io.github.chehsunliu.itx.backend.feature.user;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserDto {
  UUID id;
  String email;
}
