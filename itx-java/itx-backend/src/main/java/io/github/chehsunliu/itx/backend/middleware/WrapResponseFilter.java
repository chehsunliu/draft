package io.github.chehsunliu.itx.backend.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
@Order(1)
@RequiredArgsConstructor
public class WrapResponseFilter extends OncePerRequestFilter {

  private final ObjectMapper objectMapper;

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
      throws ServletException, IOException {
    ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(resp);
    chain.doFilter(req, wrapper);

    int status = wrapper.getStatus();
    boolean isSuccess = status >= 200 && status < 300;
    String contentType = wrapper.getContentType();
    boolean isJson =
        contentType != null && contentType.startsWith(MediaType.APPLICATION_JSON_VALUE);
    byte[] body = wrapper.getContentAsByteArray();

    // Case 2: success, non-json → pass through
    if (isSuccess && !isJson) {
      wrapper.copyBodyToResponse();
      return;
    }

    byte[] out;
    if (isJson) {
      try {
        JsonNode inner =
            (body.length == 0) ? objectMapper.createObjectNode() : objectMapper.readTree(body);
        ObjectNode wrapped = objectMapper.createObjectNode();
        wrapped.set(isSuccess ? "data" : "error", inner);
        out = objectMapper.writeValueAsBytes(wrapped);
      } catch (Exception e) {
        // If the body isn't valid JSON, pass through as-is.
        wrapper.copyBodyToResponse();
        return;
      }
    } else {
      // Case 3: error, no json → generic error envelope
      String detail = body.length == 0 ? null : new String(body, StandardCharsets.UTF_8);
      String message = (detail == null || detail.isBlank()) ? defaultMessage(status) : detail;
      ObjectNode wrapped = objectMapper.createObjectNode();
      ObjectNode err = wrapped.putObject("error");
      err.put("message", message);
      out = objectMapper.writeValueAsBytes(wrapped);
    }

    resp.resetBuffer();
    resp.setStatus(status);
    resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
    resp.setContentLength(out.length);
    resp.getOutputStream().write(out);
  }

  private static String defaultMessage(int status) {
    HttpStatus s = HttpStatus.resolve(status);
    return s != null ? s.getReasonPhrase() : "unknown error";
  }
}
