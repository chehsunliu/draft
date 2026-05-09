package io.github.chehsunliu.itx.backend.feature.subscription.usecase;

import io.github.chehsunliu.itx.backend.error.BackendException;
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import java.util.UUID;

public final class UnsubscribeUseCase {
  public record ExecuteParams(UUID subscriberId, UUID authorId) {}

  private final UserRepo userRepo;
  private final SubscriptionRepo subscriptionRepo;

  public UnsubscribeUseCase(UserRepo userRepo, SubscriptionRepo subscriptionRepo) {
    this.userRepo = userRepo;
    this.subscriptionRepo = subscriptionRepo;
  }

  public void execute(ExecuteParams params) {
    if (params.subscriberId.equals(params.authorId)) {
      throw BackendException.badRequest("cannot unsubscribe from yourself");
    }
    userRepo.get(params.authorId);
    subscriptionRepo.unsubscribe(
        new SubscriptionRepo.UnsubscribeParams(params.subscriberId, params.authorId));
  }
}
