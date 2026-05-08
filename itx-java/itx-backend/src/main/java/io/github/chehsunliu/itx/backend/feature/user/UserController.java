package io.github.chehsunliu.itx.backend.feature.user;

import io.github.chehsunliu.itx.backend.error.BackendException;
import io.github.chehsunliu.itx.backend.middleware.ItxContext;
import io.github.chehsunliu.itx.backend.service.SubscriptionService;
import io.github.chehsunliu.itx.backend.service.UserService;
import io.github.chehsunliu.itx.contract.repo.User;
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

  private final UserService userService;
  private final SubscriptionService subscriptionService;
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
    User user = userService.upsert(ctx.getUserId(), ctx.getUserEmail());
    return mapper.toDto(user);
  }

  @GetMapping("/{id}/subscriptions")
  ListSubscriptionsResponse listSubscriptions(@PathVariable UUID id) {
    // Pre-check the subject so an unknown user yields 404, not an empty list.
    userService.get(id);
    List<User> authors = subscriptionService.listAuthors(id);
    return ListSubscriptionsResponse.builder()
        .items(authors.stream().map(mapper::toDto).toList())
        .build();
  }
}
