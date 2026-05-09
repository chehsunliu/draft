package io.github.chehsunliu.itx.backend.middleware;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Transparent response wrapper. Mirrors the wrap_response middleware in itx-rs: handlers just call
 * {@code ctx.json(dto)} or set a plaintext error body, and this hook rewrites the response on the
 * way out — successes become {@code {"data": <body>}}, errors become {@code {"error": <body>}} (or
 * {@code {"error": {"message": "<text>"}}} when the body isn't already JSON).
 */
@Slf4j
public final class Envelope {
    private Envelope() {}

    public static void install(Javalin app, ObjectMapper mapper) {
        app.after(ctx -> {
            try {
                wrap(ctx, mapper);
            } catch (Exception e) {
                log.warn("envelope wrap failed", e);
            }
        });
    }

    private static void wrap(Context ctx, ObjectMapper mapper) throws Exception {
        int status = ctx.statusCode();
        boolean isSuccess = status >= 200 && status < 300;
        String contentType = ctx.res().getContentType();
        boolean isJson = contentType != null && contentType.startsWith("application/json");
        String body = ctx.result();
        if (body == null) body = "";

        // Success without a JSON body (e.g., 204 No Content) passes through.
        if (isSuccess && !isJson) return;

        if (isJson && !body.isEmpty()) {
            JsonNode inner = mapper.readTree(body);
            String key = isSuccess ? "data" : "error";
            ctx.contentType("application/json").result(mapper.writeValueAsString(Map.of(key, inner)));
            return;
        }

        if (!isSuccess) {
            String message;
            if (!body.isEmpty()) {
                message = body;
            } else {
                HttpStatus s = HttpStatus.forStatus(status);
                message = s == HttpStatus.UNKNOWN ? "error" : s.getMessage();
            }
            ctx.contentType("application/json")
                    .result(mapper.writeValueAsString(Map.of("error", Map.of("message", message))));
        }
    }
}
