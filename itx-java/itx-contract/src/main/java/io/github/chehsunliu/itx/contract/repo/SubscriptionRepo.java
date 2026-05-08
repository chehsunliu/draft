package io.github.chehsunliu.itx.contract.repo;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

public interface SubscriptionRepo {

  /** Inserts the subscription if missing. Idempotent. */
  void subscribe(SubscribeParams params);

  /** Removes the subscription if present. Idempotent. */
  void unsubscribe(UnsubscribeParams params);

  /**
   * Returns the list of authors that {@code subscriberId} follows, ordered by most recently
   * subscribed first.
   */
  List<User> listAuthors(UUID subscriberId);

  /**
   * Returns the list of users subscribed to {@code authorId}, ordered by most recently subscribed
   * first.
   */
  List<User> listSubscribers(UUID authorId);

  @Value
  @Builder
  class SubscribeParams {
    UUID subscriberId;
    UUID authorId;
  }

  @Value
  @Builder
  class UnsubscribeParams {
    UUID subscriberId;
    UUID authorId;
  }
}
