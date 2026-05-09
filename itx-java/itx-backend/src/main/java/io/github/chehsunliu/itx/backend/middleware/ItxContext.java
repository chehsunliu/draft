package io.github.chehsunliu.itx.backend.middleware;

import io.github.chehsunliu.itx.backend.error.BackendException;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.UUID;

public final class ItxContext {
    public static final String HEADER_REQUEST_ID = "X-Itx-Request-Id";
    public static final String HEADER_USER_ID = "X-Itx-User-Id";
    public static final String HEADER_USER_EMAIL = "X-Itx-User-Email";

    private static final String ATTR = "itx.context";

    public final UUID requestId;
    public final UUID userId;
    public final String userEmail;

    public ItxContext(UUID requestId, UUID userId, String userEmail) {
        this.requestId = requestId;
        this.userId = userId;
        this.userEmail = userEmail;
    }

    public static void install(Javalin app) {
        app.before(ctx -> {
            UUID requestId;
            try {
                String raw = ctx.header(HEADER_REQUEST_ID);
                requestId = raw == null ? UUID.randomUUID() : UUID.fromString(raw);
            } catch (IllegalArgumentException e) {
                throw new BackendException(400, "invalid " + HEADER_REQUEST_ID);
            }
            UUID userId;
            try {
                String raw = ctx.header(HEADER_USER_ID);
                userId = raw == null ? null : UUID.fromString(raw);
            } catch (IllegalArgumentException e) {
                throw new BackendException(400, "invalid " + HEADER_USER_ID);
            }
            String userEmail = ctx.header(HEADER_USER_EMAIL);
            ctx.attribute(ATTR, new ItxContext(requestId, userId, userEmail));
        });
    }

    public static ItxContext from(Context ctx) {
        return ctx.attribute(ATTR);
    }
}
