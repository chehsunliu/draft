package io.github.chehsunliu.itx.backend.feature.user;

import io.github.chehsunliu.itx.backend.AppState;
import io.github.chehsunliu.itx.backend.error.BackendException;
import io.github.chehsunliu.itx.backend.feature.user.usecase.GetMeUseCase;
import io.github.chehsunliu.itx.backend.feature.user.usecase.ListSubscriptionsUseCase;
import io.github.chehsunliu.itx.backend.middleware.ItxContext;
import io.javalin.Javalin;
import java.util.UUID;

public final class UserRoutes {
  private UserRoutes() {}

  public static void register(Javalin app, AppState state) {
    app.get(
        "/api/v1/users/me",
        ctx -> {
          ItxContext c = ItxContext.from(ctx);
          if (c.userEmail == null) throw BackendException.unknown("missing X-Itx-User-Email");
          UserDto dto = state.getMe.execute(new GetMeUseCase.ExecuteParams(c.userId, c.userEmail));
          ctx.json(dto);
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
          ListSubscriptionsUseCase.ExecuteOutput out =
              state.listSubscriptions.execute(new ListSubscriptionsUseCase.ExecuteParams(id));
          ctx.json(out);
        });
  }
}
