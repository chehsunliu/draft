package io.github.chehsunliu.itx.backend.service;

import io.github.chehsunliu.itx.contract.repo.User;
import io.github.chehsunliu.itx.impl.repo.entity.SubscriptionId;
import io.github.chehsunliu.itx.impl.repo.jpa.SubscriptionJpaRepo;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

  private final SubscriptionJpaRepo subscriptionRepo;
  private final IdempotentInserter inserter;

  @Transactional
  public void subscribe(UUID subscriberId, UUID authorId) {
    SubscriptionId id = new SubscriptionId(subscriberId, authorId);
    if (subscriptionRepo.existsById(id)) {
      return;
    }
    inserter.insertSubscriptionIfAbsent(id);
  }

  @Transactional
  public void unsubscribe(UUID subscriberId, UUID authorId) {
    SubscriptionId id = new SubscriptionId(subscriberId, authorId);
    if (subscriptionRepo.existsById(id)) {
      subscriptionRepo.deleteById(id);
    }
  }

  @Transactional(readOnly = true)
  public List<User> listAuthors(UUID subscriberId) {
    return subscriptionRepo.listAuthorsBySubscriberId(subscriberId).stream()
        .map(UserService::toContract)
        .toList();
  }
}
