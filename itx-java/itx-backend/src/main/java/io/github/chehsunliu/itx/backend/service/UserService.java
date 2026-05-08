package io.github.chehsunliu.itx.backend.service;

import io.github.chehsunliu.itx.backend.error.BackendException;
import io.github.chehsunliu.itx.contract.repo.User;
import io.github.chehsunliu.itx.impl.repo.entity.UserEntity;
import io.github.chehsunliu.itx.impl.repo.jpa.UserJpaRepo;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserJpaRepo userRepo;
  private final IdempotentInserter inserter;

  @Transactional
  public User upsert(UUID id, String email) {
    UserEntity entity =
        userRepo.findById(id).orElseGet(() -> inserter.insertUserIfAbsent(id, email));
    return toContract(entity);
  }

  @Transactional(readOnly = true)
  public User get(UUID id) {
    return userRepo
        .findById(id)
        .map(UserService::toContract)
        .orElseThrow(BackendException::notFound);
  }

  static User toContract(UserEntity e) {
    return User.builder().id(e.getId()).email(e.getEmail()).build();
  }
}
