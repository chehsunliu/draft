package io.github.chehsunliu.itx.backend.feature.user;

import io.github.chehsunliu.itx.contract.repo.User;
import java.util.UUID;

public record UserDto(UUID id, String email) {
  public static UserDto fromUser(User u) {
    return new UserDto(u.id(), u.email());
  }
}
