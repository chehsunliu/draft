package io.github.chehsunliu.itx.contract.repo;

import java.util.List;
import java.util.UUID;

public interface SubscriptionRepo {
  /** Inserts the subscription if missing. Idempotent. */
  void subscribe(SubscribeParams params);

  /** Removes the subscription if present. Idempotent. */
  void unsubscribe(UnsubscribeParams params);

  /** Authors that {@code subscriberId} follows, ordered by most-recently subscribed first. */
  List<User> listAuthors(UUID subscriberId);

  /** Users subscribed to {@code authorId}, ordered by most-recently subscribed first. */
  List<User> listSubscribers(UUID authorId);

  record SubscribeParams(UUID subscriberId, UUID authorId) {}

  record UnsubscribeParams(UUID subscriberId, UUID authorId) {}
}
