package io.github.chehsunliu.itx.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chehsunliu.itx.backend.error.BackendException;
import io.github.chehsunliu.itx.backend.feature.health.HealthRoutes;
import io.github.chehsunliu.itx.backend.feature.post.PostRoutes;
import io.github.chehsunliu.itx.backend.feature.subscription.SubscriptionRoutes;
import io.github.chehsunliu.itx.backend.feature.user.UserRoutes;
import io.github.chehsunliu.itx.backend.middleware.Envelope;
import io.github.chehsunliu.itx.backend.middleware.ItxContext;
import io.github.chehsunliu.itx.contract.queue.QueueException;
import io.github.chehsunliu.itx.contract.repo.RepoNotFoundException;
import io.javalin.Javalin;
import io.javalin.http.util.MethodNotAllowedUtil;
import io.javalin.json.JavalinJackson;

public final class Module {
  private Module() {}

  public static Javalin build(AppState state) {
    ObjectMapper mapper = Json.newMapper();
    Javalin app =
        Javalin.create(
            cfg -> {
              cfg.showJavalinBanner = false;
              cfg.jsonMapper(new JavalinJackson(mapper, false));
            });

    ItxContext.install(app);

    // Auth gate: runs only after a handler matches, so unmatched paths still return 404 from
    // the router — the same behavior as the Kotlin/Ktor RequireUser plugin.
    app.beforeMatched(
        ctx -> {
          String path = ctx.path();
          if (path.equals("/api/v1/health")) return;
          ItxContext c = ItxContext.from(ctx);
          if (c == null || c.userId == null) {
            Envelope.respondError(ctx, mapper, 401, "Unauthorized");
            ctx.skipRemainingHandlers();
          }
        });

    HealthRoutes.register(app, mapper);
    PostRoutes.register(app, mapper, state.postRepo, state.controlStandardQueue);
    UserRoutes.register(app, mapper, state.userRepo, state.subscriptionRepo);
    SubscriptionRoutes.register(app, mapper, state.userRepo, state.subscriptionRepo);

    app.exception(
        BackendException.class,
        (e, ctx) -> Envelope.respondError(ctx, mapper, e.status(), e.getMessage()));
    app.exception(
        RepoNotFoundException.class,
        (e, ctx) -> Envelope.respondError(ctx, mapper, 404, "not found"));
    app.exception(
        QueueException.class,
        (e, ctx) ->
            Envelope.respondError(
                ctx, mapper, 500, e.getMessage() == null ? "queue error" : e.getMessage()));
    app.exception(
        Exception.class,
        (e, ctx) ->
            Envelope.respondError(
                ctx, mapper, 500, e.getMessage() == null ? "internal error" : e.getMessage()));

    // Error mappers run after exception handlers and would otherwise overwrite their bodies.
    // Skip the override when an exception handler already wrote a JSON envelope; otherwise
    // promote the unmatched-route default to a 405 if the path is registered for another
    // method, and fall back to 404.
    app.error(
        404,
        ctx -> {
          if (isJsonBody(ctx)) return;
          var available =
              MethodNotAllowedUtil.INSTANCE.findAvailableHttpHandlerTypes(
                  app.unsafeConfig().pvt.internalRouter, ctx.path());
          if (!available.isEmpty()) {
            Envelope.respondError(ctx, mapper, 405, "Method Not Allowed");
          } else {
            Envelope.respondError(ctx, mapper, 404, "Not Found");
          }
        });
    app.error(
        405,
        ctx -> {
          if (!isJsonBody(ctx)) Envelope.respondError(ctx, mapper, 405, "Method Not Allowed");
        });
    app.error(
        401,
        ctx -> {
          if (!isJsonBody(ctx)) Envelope.respondError(ctx, mapper, 401, "Unauthorized");
        });

    return app;
  }

  private static boolean isJsonBody(io.javalin.http.Context ctx) {
    String body = ctx.result();
    if (body == null || body.isEmpty()) return false;
    String type = ctx.res().getContentType();
    return type != null && type.startsWith("application/json");
  }
}
