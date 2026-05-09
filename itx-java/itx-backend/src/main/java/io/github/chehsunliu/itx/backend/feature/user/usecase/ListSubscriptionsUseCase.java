package io.github.chehsunliu.itx.backend.feature.user.usecase;

import io.github.chehsunliu.itx.backend.feature.user.UserDto;
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.User;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ListSubscriptionsUseCase {
    public record ExecuteParams(UUID subscriberId) {}

    public record ExecuteOutput(List<UserDto> items) {}

    private final UserRepo userRepo;
    private final SubscriptionRepo subscriptionRepo;

    public ExecuteOutput execute(ExecuteParams params) {
        // Pre-check the subject so an unknown user yields 404, not an empty list.
        userRepo.get(params.subscriberId);
        List<User> authors = subscriptionRepo.listAuthors(params.subscriberId);
        return new ExecuteOutput(authors.stream().map(UserDto::fromUser).collect(Collectors.toList()));
    }
}
