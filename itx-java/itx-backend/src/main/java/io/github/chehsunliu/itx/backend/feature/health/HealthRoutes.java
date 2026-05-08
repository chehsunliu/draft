package io.github.chehsunliu.itx.backend.feature.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chehsunliu.itx.backend.middleware.Envelope;
import io.javalin.Javalin;
import java.util.Map;

public final class HealthRoutes {
  private HealthRoutes() {}

  public static void register(Javalin app, ObjectMapper mapper) {
    app.get("/api/v1/health", ctx -> Envelope.respondData(ctx, mapper, Map.of("status", "ok")));
  }
}
