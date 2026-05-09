package io.github.chehsunliu.itx.backend.feature.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chehsunliu.itx.backend.error.BackendException;
import io.github.chehsunliu.itx.backend.middleware.Envelope;
import io.github.chehsunliu.itx.backend.middleware.ItxContext;
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.User;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import io.javalin.Javalin;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class UserRoutes {
  private UserRoutes() {}

  public static void register(
      Javalin app, ObjectMapper mapper, UserRepo userRepo, SubscriptionRepo subscriptionRepo) {
    app.get(
        "/api/v1/users/me",
        ctx -> {
          ItxContext c = ItxContext.from(ctx);
          if (c.userEmail == null) throw BackendException.unknown("missing X-Itx-User-Email");
          User user = userRepo.upsert(new UserRepo.UpsertParams(c.userId, c.userEmail));
          Envelope.data(ctx, UserDto.fromUser(user));
        });

    app.get(
        "/api/v1/users/{id}/subscriptions",
        ctx -> {
          UUID id;
          try {
            id = UUID.fromString(ctx.pathParam("id"));
          } catch (IllegalArgumentException e) {
            throw BackendException.badRequest("invalid user id");
          }
          // Pre-check the subject so an unknown user yields 404, not an empty list.
          userRepo.get(id);
          List<User> authors = subscriptionRepo.listAuthors(id);
          List<UserDto> items =
              authors.stream().map(UserDto::fromUser).collect(Collectors.toList());
          Envelope.data(ctx, Map.of("items", items));
        });
  }
}
