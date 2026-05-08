package io.github.chehsunliu.itx.backend.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(10)
public class ItxContextFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
      throws ServletException, IOException {
    UUID requestId;
    try {
      String raw = req.getHeader(ItxContext.HEADER_REQUEST_ID);
      requestId = (raw == null) ? UUID.randomUUID() : UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      resp.setContentType("application/json");
      resp.getWriter()
          .write("{\"error\":{\"message\":\"invalid " + ItxContext.HEADER_REQUEST_ID + "\"}}");
      return;
    }

    UUID userId;
    try {
      String raw = req.getHeader(ItxContext.HEADER_USER_ID);
      userId = (raw == null) ? null : UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      resp.setContentType("application/json");
      resp.getWriter()
          .write("{\"error\":{\"message\":\"invalid " + ItxContext.HEADER_USER_ID + "\"}}");
      return;
    }

    String userEmail = req.getHeader(ItxContext.HEADER_USER_EMAIL);

    ItxContext ctx =
        ItxContext.builder().requestId(requestId).userId(userId).userEmail(userEmail).build();
    req.setAttribute(ItxContext.ATTR, ctx);
    chain.doFilter(req, resp);
  }
}
