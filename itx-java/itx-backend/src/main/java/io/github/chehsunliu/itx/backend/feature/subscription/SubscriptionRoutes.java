package io.github.chehsunliu.itx.backend.feature.subscription;

import io.github.chehsunliu.itx.backend.AppState;
import io.github.chehsunliu.itx.backend.error.BackendException;
import io.github.chehsunliu.itx.backend.feature.subscription.usecase.SubscribeUseCase;
import io.github.chehsunliu.itx.backend.feature.subscription.usecase.UnsubscribeUseCase;
import io.github.chehsunliu.itx.backend.middleware.ItxContext;
import io.javalin.Javalin;
import java.util.UUID;

public final class SubscriptionRoutes {
    private SubscriptionRoutes() {}

    public static void register(Javalin app, AppState state) {
        app.put("/api/v1/subscriptions/{authorId}", ctx -> {
            ItxContext c = ItxContext.from(ctx);
            UUID authorId = parseAuthorId(ctx.pathParam("authorId"));
            if (c.userEmail == null) throw BackendException.unknown("missing X-Itx-User-Email");
            state.subscribe.execute(new SubscribeUseCase.ExecuteParams(c.userId, c.userEmail, authorId));
            ctx.status(204);
        });

        app.delete("/api/v1/subscriptions/{authorId}", ctx -> {
            ItxContext c = ItxContext.from(ctx);
            UUID authorId = parseAuthorId(ctx.pathParam("authorId"));
            state.unsubscribe.execute(new UnsubscribeUseCase.ExecuteParams(c.userId, authorId));
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
