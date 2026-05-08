package io.github.chehsunliu.itx.backend.feature.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chehsunliu.itx.backend.error.BackendException;
import io.github.chehsunliu.itx.backend.middleware.ItxContext;
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import io.javalin.Javalin;
import java.util.UUID;

public final class SubscriptionRoutes {
  private SubscriptionRoutes() {}

  public static void register(
      Javalin app, ObjectMapper mapper, UserRepo userRepo, SubscriptionRepo subscriptionRepo) {
    app.put(
        "/api/v1/subscriptions/{authorId}",
        ctx -> {
          ItxContext c = ItxContext.from(ctx);
          UUID authorId = parseAuthorId(ctx.pathParam("authorId"));
          if (authorId.equals(c.userId)) {
            throw BackendException.badRequest("cannot subscribe to yourself");
          }
          if (c.userEmail == null) throw BackendException.unknown("missing X-Itx-User-Email");
          // Pre-check the target so we return 404 cleanly instead of an FK violation.
          userRepo.get(authorId);
          // Ensure the subscriber row exists; safe to call before /me has ever been hit.
          userRepo.upsert(new UserRepo.UpsertParams(c.userId, c.userEmail));
          subscriptionRepo.subscribe(new SubscriptionRepo.SubscribeParams(c.userId, authorId));
          ctx.status(204);
        });

    app.delete(
        "/api/v1/subscriptions/{authorId}",
        ctx -> {
          ItxContext c = ItxContext.from(ctx);
          UUID authorId = parseAuthorId(ctx.pathParam("authorId"));
          if (authorId.equals(c.userId)) {
            throw BackendException.badRequest("cannot unsubscribe from yourself");
          }
          userRepo.get(authorId);
          subscriptionRepo.unsubscribe(new SubscriptionRepo.UnsubscribeParams(c.userId, authorId));
          ctx.status(204);
        });
  }

  private static UUID parseAuthorId(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw BackendException.badRequest("invalid author id");
    }
  }
}
