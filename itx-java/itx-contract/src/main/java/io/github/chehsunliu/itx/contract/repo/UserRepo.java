package io.github.chehsunliu.itx.contract.repo;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

public interface UserRepo {

  /** Inserts the user if missing, then returns the current row. */
  User upsert(UpsertParams params);

  /** Throws {@link RepoNotFoundException} if no user with {@code id} exists. */
  User get(UUID id);

  @Value
  @Builder
  class UpsertParams {
    UUID id;
    String email;
  }
}
