package io.github.chehsunliu.itx.backend.feature.subscription.usecase;

import io.github.chehsunliu.itx.backend.error.BackendException;
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class SubscribeUseCase {
    public record ExecuteParams(UUID subscriberId, String subscriberEmail, UUID authorId) {}

    private final UserRepo userRepo;
    private final SubscriptionRepo subscriptionRepo;

    public void execute(ExecuteParams params) {
        if (params.subscriberId.equals(params.authorId)) {
            throw BackendException.badRequest("cannot subscribe to yourself");
        }
        // Pre-check the target so we return 404 cleanly instead of an FK violation.
        userRepo.get(params.authorId);
        // Ensure the subscriber row exists; safe to call before /me has ever been hit.
        userRepo.upsert(new UserRepo.UpsertParams(params.subscriberId, params.subscriberEmail));
        subscriptionRepo.subscribe(new SubscriptionRepo.SubscribeParams(params.subscriberId, params.authorId));
    }
}
