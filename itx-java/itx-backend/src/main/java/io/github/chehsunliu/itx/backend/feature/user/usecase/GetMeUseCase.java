package io.github.chehsunliu.itx.backend.feature.user.usecase;

import io.github.chehsunliu.itx.backend.feature.user.UserDto;
import io.github.chehsunliu.itx.contract.repo.User;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import java.util.UUID;

public final class GetMeUseCase {
  public record ExecuteParams(UUID userId, String email) {}

  private final UserRepo userRepo;

  public GetMeUseCase(UserRepo userRepo) {
    this.userRepo = userRepo;
  }

  public UserDto execute(ExecuteParams params) {
    User user = userRepo.upsert(new UserRepo.UpsertParams(params.userId, params.email));
    return UserDto.fromUser(user);
  }
}
