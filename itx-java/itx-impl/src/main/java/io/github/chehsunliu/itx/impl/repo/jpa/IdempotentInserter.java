package io.github.chehsunliu.itx.impl.repo.jpa;

import io.github.chehsunliu.itx.impl.repo.entity.SubscriptionEntity;
import io.github.chehsunliu.itx.impl.repo.entity.SubscriptionId;
import io.github.chehsunliu.itx.impl.repo.entity.TagEntity;
import io.github.chehsunliu.itx.impl.repo.entity.UserEntity;
import io.github.chehsunliu.itx.impl.repo.jpa.data.SubscriptionEntityRepository;
import io.github.chehsunliu.itx.impl.repo.jpa.data.TagEntityRepository;
import io.github.chehsunliu.itx.impl.repo.jpa.data.UserEntityRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inserts that need to be race-safe live in their own short-lived transaction so a unique-key
 * collision rolls back only the inner attempt and the caller's outer transaction stays clean. On
 * conflict we re-read the now-existing row.
 */
@RequiredArgsConstructor
public class IdempotentInserter {

  private final UserEntityRepository userRepo;
  private final TagEntityRepository tagRepo;
  private final SubscriptionEntityRepository subscriptionRepo;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public UserEntity insertUserIfAbsent(UUID id, String email) {
    try {
      return userRepo.save(new UserEntity(id, email));
    } catch (DataIntegrityViolationException dup) {
      return userRepo.findById(id).orElseThrow(() -> dup);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public TagEntity insertTagIfAbsent(String name) {
    try {
      return tagRepo.save(new TagEntity(name));
    } catch (DataIntegrityViolationException dup) {
      return tagRepo.findByName(name).orElseThrow(() -> dup);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void insertSubscriptionIfAbsent(SubscriptionId id) {
    try {
      subscriptionRepo.save(new SubscriptionEntity(id));
    } catch (DataIntegrityViolationException ignored) {
      // already subscribed; idempotent no-op.
    }
  }
}
