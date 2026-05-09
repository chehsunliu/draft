package io.github.chehsunliu.itx.backend.feature.health;

import io.javalin.Javalin;
import java.util.Map;

public final class HealthRoutes {
    private HealthRoutes() {}

    public static void register(Javalin app) {
        app.get("/api/v1/health", ctx -> ctx.json(Map.of("status", "ok")));
    }
}
