package io.github.chehsunliu.itx.backend.middleware;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequireUserInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
    ItxContext ctx = (ItxContext) req.getAttribute(ItxContext.ATTR);
    if (ctx == null || ctx.getUserId() == null) {
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return false;
    }
    return true;
  }
}
