package io.github.chehsunliu.itx.backend.feature.user;

import io.github.chehsunliu.itx.backend.error.BackendException;
import io.github.chehsunliu.itx.backend.middleware.ItxContext;
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.User;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserRepo userRepo;
  private final SubscriptionRepo subscriptionRepo;
  private final UserMapper mapper;

  @Value
  @Builder
  static class ListSubscriptionsResponse {
    List<UserDto> items;
  }

  @GetMapping("/me")
  UserDto me(HttpServletRequest req) {
    ItxContext ctx = (ItxContext) req.getAttribute(ItxContext.ATTR);
    if (ctx.getUserEmail() == null) {
      throw BackendException.unknown("missing X-Itx-User-Email");
    }
    User user =
        userRepo.upsert(
            UserRepo.UpsertParams.builder().id(ctx.getUserId()).email(ctx.getUserEmail()).build());
    return mapper.toDto(user);
  }

  @GetMapping("/{id}/subscriptions")
  ListSubscriptionsResponse listSubscriptions(@PathVariable UUID id) {
    // Pre-check the subject so an unknown user yields 404, not an empty list.
    userRepo.get(id);
    List<User> authors = subscriptionRepo.listAuthors(id);
    return ListSubscriptionsResponse.builder()
        .items(authors.stream().map(mapper::toDto).toList())
        .build();
  }
}
