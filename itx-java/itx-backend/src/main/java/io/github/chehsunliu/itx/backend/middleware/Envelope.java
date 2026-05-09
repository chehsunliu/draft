package io.github.chehsunliu.itx.backend.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.Map;

/**
 * Wraps the success-arm response in {@code { "data": <payload> }}. Handlers stash the payload via
 * {@link #data(Context, Object)}; an {@code afterMatched} hook installed by {@link
 * #install(Javalin, ObjectMapper)} serializes the wrapped envelope. Mirrors the success arm of the
 * wrap_response middleware in itx-rs.
 */
public final class Envelope {
  private static final String DATA_ATTR = "itx.envelope.data";

  private Envelope() {}

  public static void install(Javalin app, ObjectMapper mapper) {
    app.afterMatched(
        ctx -> {
          Object data = ctx.attribute(DATA_ATTR);
          if (data == null) return;
          // Exception handlers may have overridden the response with an error body; skip
          // wrapping so we don't clobber it.
          if (ctx.statusCode() >= 400) return;
          ctx.contentType("application/json")
              .result(mapper.writeValueAsString(Map.of("data", data)));
        });
  }

  public static void data(Context ctx, Object value) {
    ctx.attribute(DATA_ATTR, value);
  }

  public static void data(Context ctx, Object value, int status) {
    ctx.status(status).attribute(DATA_ATTR, value);
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
