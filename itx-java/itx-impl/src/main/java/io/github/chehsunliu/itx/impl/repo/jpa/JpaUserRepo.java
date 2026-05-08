package io.github.chehsunliu.itx.impl.repo.jpa;

import io.github.chehsunliu.itx.contract.repo.RepoNotFoundException;
import io.github.chehsunliu.itx.contract.repo.User;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import io.github.chehsunliu.itx.impl.repo.entity.UserEntity;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class JpaUserRepo implements UserRepo {

  private final UserJpaRepo userJpaRepo;
  private final IdempotentInserter inserter;

  @Override
  @Transactional
  public User upsert(UpsertParams params) {
    UserEntity entity =
        userJpaRepo
            .findById(params.getId())
            .orElseGet(() -> inserter.insertUserIfAbsent(params.getId(), params.getEmail()));
    return toContract(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public User get(UUID id) {
    return userJpaRepo
        .findById(id)
        .map(JpaUserRepo::toContract)
        .orElseThrow(RepoNotFoundException::new);
  }

  static User toContract(UserEntity e) {
    return User.builder().id(e.getId()).email(e.getEmail()).build();
  }
}
