package io.github.chehsunliu.itx.backend.feature.subscription;

import io.github.chehsunliu.itx.backend.error.BackendException;
import io.github.chehsunliu.itx.backend.middleware.ItxContext;
import io.github.chehsunliu.itx.backend.service.SubscriptionService;
import io.github.chehsunliu.itx.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

  private final UserService userService;
  private final SubscriptionService subscriptionService;

  @PutMapping("/{authorId}")
  ResponseEntity<Void> subscribe(HttpServletRequest req, @PathVariable UUID authorId) {
    ItxContext ctx = (ItxContext) req.getAttribute(ItxContext.ATTR);
    if (ctx.getUserId().equals(authorId)) {
      throw BackendException.badRequest("cannot subscribe to yourself");
    }
    if (ctx.getUserEmail() == null) {
      throw BackendException.unknown("missing X-Itx-User-Email");
    }
    // Pre-check the target so we return 404 cleanly instead of an FK violation.
    userService.get(authorId);
    // Ensure the subscriber row exists; safe to call before /me has ever been hit.
    userService.upsert(ctx.getUserId(), ctx.getUserEmail());
    subscriptionService.subscribe(ctx.getUserId(), authorId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{authorId}")
  ResponseEntity<Void> unsubscribe(HttpServletRequest req, @PathVariable UUID authorId) {
    ItxContext ctx = (ItxContext) req.getAttribute(ItxContext.ATTR);
    if (ctx.getUserId().equals(authorId)) {
      throw BackendException.badRequest("cannot unsubscribe from yourself");
    }
    userService.get(authorId);
    subscriptionService.unsubscribe(ctx.getUserId(), authorId);
    return ResponseEntity.noContent().build();
  }
}
