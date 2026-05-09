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

  public static Javalin build(AppState state, ObjectMapper mapper) {
    Javalin app =
        Javalin.create(
            cfg -> {
              cfg.showJavalinBanner = false;
              cfg.jsonMapper(new JavalinJackson(mapper, false));
            });

    ItxContext.install(app);

    // Auth gate: runs only after a route matches, so unmatched paths still return 404 from the
    // router. Throws into the exception handler chain so the wrap middleware sees a plain
    // status+body to wrap on the way out.
    app.beforeMatched(
        ctx -> {
          if (ctx.path().equals("/api/v1/health")) return;
          ItxContext c = ItxContext.from(ctx);
          if (c == null || c.userId == null) {
            throw new BackendException(401, "Unauthorized");
          }
        });

    HealthRoutes.register(app);
    PostRoutes.register(app, mapper, state);
    UserRoutes.register(app, state);
    SubscriptionRoutes.register(app, state);

    app.exception(
        BackendException.class, (e, ctx) -> ctx.status(e.status()).result(e.getMessage()));
    app.exception(RepoNotFoundException.class, (e, ctx) -> ctx.status(404).result("not found"));
    app.exception(
        QueueException.class,
        (e, ctx) ->
            ctx.status(500).result(e.getMessage() == null ? "queue error" : e.getMessage()));
    app.exception(
        Exception.class,
        (e, ctx) ->
            ctx.status(500).result(e.getMessage() == null ? "internal error" : e.getMessage()));

    // Javalin's default 404 body is the plaintext "Endpoint X not found". Replace it with a
    // clean "Not Found" message — or promote to 405 when the path is registered for another
    // method — so the wrap middleware emits a tidy envelope.
    app.error(
        404,
        ctx -> {
          String body = ctx.result();
          if (body == null || !body.startsWith("Endpoint ")) return;
          var available =
              MethodNotAllowedUtil.INSTANCE.findAvailableHttpHandlerTypes(
                  app.unsafeConfig().pvt.internalRouter, ctx.path());
          if (!available.isEmpty()) {
            ctx.status(405).result("Method Not Allowed");
          } else {
            ctx.result("Not Found");
          }
        });

    Envelope.install(app, mapper);

    return app;
  }
}
