package io.github.chehsunliu.itx.backend.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import java.util.Map;

/** Wraps a payload as {@code { "data": <payload> }}. Mirrors the success-response envelope. */
public final class Envelope {
  private Envelope() {}

  public static void respondData(Context ctx, ObjectMapper mapper, Object value, int status) {
    try {
      ctx.status(status)
          .contentType("application/json")
          .result(mapper.writeValueAsString(Map.of("data", value)));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void respondData(Context ctx, ObjectMapper mapper, Object value) {
    respondData(ctx, mapper, value, 200);
  }

  public static void respondError(Context ctx, ObjectMapper mapper, int status, String message) {
    try {
      ctx.status(status)
          .contentType("application/json")
          .result(mapper.writeValueAsString(Map.of("error", Map.of("message", message))));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
