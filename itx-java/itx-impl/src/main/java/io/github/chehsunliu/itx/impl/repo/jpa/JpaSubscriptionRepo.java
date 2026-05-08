package io.github.chehsunliu.itx.impl.repo.jpa;

import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.User;
import io.github.chehsunliu.itx.impl.repo.entity.SubscriptionId;
import io.github.chehsunliu.itx.impl.repo.jpa.data.SubscriptionEntityRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class JpaSubscriptionRepo implements SubscriptionRepo {

  private final SubscriptionEntityRepository subscriptionEntityRepo;
  private final IdempotentInserter inserter;

  @Override
  @Transactional
  public void subscribe(SubscribeParams params) {
    SubscriptionId id = new SubscriptionId(params.getSubscriberId(), params.getAuthorId());
    if (subscriptionEntityRepo.existsById(id)) {
      return;
    }
    inserter.insertSubscriptionIfAbsent(id);
  }

  @Override
  @Transactional
  public void unsubscribe(UnsubscribeParams params) {
    SubscriptionId id = new SubscriptionId(params.getSubscriberId(), params.getAuthorId());
    if (subscriptionEntityRepo.existsById(id)) {
      subscriptionEntityRepo.deleteById(id);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> listAuthors(UUID subscriberId) {
    return subscriptionEntityRepo.listAuthorsBySubscriberId(subscriberId).stream()
        .map(JpaUserRepo::toContract)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> listSubscribers(UUID authorId) {
    return subscriptionEntityRepo.listSubscribersByAuthorId(authorId).stream()
        .map(JpaUserRepo::toContract)
        .toList();
  }
}
