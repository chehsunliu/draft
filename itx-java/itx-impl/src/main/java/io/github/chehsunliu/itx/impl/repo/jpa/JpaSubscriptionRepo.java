package io.github.chehsunliu.itx.impl.repo.jpa;

import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.User;
import io.github.chehsunliu.itx.impl.repo.entity.SubscriptionId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class JpaSubscriptionRepo implements SubscriptionRepo {

  private final SubscriptionJpaRepo subscriptionJpaRepo;
  private final IdempotentInserter inserter;

  @Override
  @Transactional
  public void subscribe(SubscribeParams params) {
    SubscriptionId id = new SubscriptionId(params.getSubscriberId(), params.getAuthorId());
    if (subscriptionJpaRepo.existsById(id)) {
      return;
    }
    inserter.insertSubscriptionIfAbsent(id);
  }

  @Override
  @Transactional
  public void unsubscribe(UnsubscribeParams params) {
    SubscriptionId id = new SubscriptionId(params.getSubscriberId(), params.getAuthorId());
    if (subscriptionJpaRepo.existsById(id)) {
      subscriptionJpaRepo.deleteById(id);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> listAuthors(UUID subscriberId) {
    return subscriptionJpaRepo.listAuthorsBySubscriberId(subscriberId).stream()
        .map(JpaUserRepo::toContract)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> listSubscribers(UUID authorId) {
    return subscriptionJpaRepo.listSubscribersByAuthorId(authorId).stream()
        .map(JpaUserRepo::toContract)
        .toList();
  }
}
