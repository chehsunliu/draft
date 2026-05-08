package io.github.chehsunliu.itx.backend.feature.subscription;

import io.github.chehsunliu.itx.backend.error.BackendException;
import io.github.chehsunliu.itx.backend.middleware.ItxContext;
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
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

  private final UserRepo userRepo;
  private final SubscriptionRepo subscriptionRepo;

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
    userRepo.get(authorId);
    // Ensure the subscriber row exists; safe to call before /me has ever been hit.
    userRepo.upsert(
        UserRepo.UpsertParams.builder().id(ctx.getUserId()).email(ctx.getUserEmail()).build());
    subscriptionRepo.subscribe(
        SubscriptionRepo.SubscribeParams.builder()
            .subscriberId(ctx.getUserId())
            .authorId(authorId)
            .build());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{authorId}")
  ResponseEntity<Void> unsubscribe(HttpServletRequest req, @PathVariable UUID authorId) {
    ItxContext ctx = (ItxContext) req.getAttribute(ItxContext.ATTR);
    if (ctx.getUserId().equals(authorId)) {
      throw BackendException.badRequest("cannot unsubscribe from yourself");
    }
    userRepo.get(authorId);
    subscriptionRepo.unsubscribe(
        SubscriptionRepo.UnsubscribeParams.builder()
            .subscriberId(ctx.getUserId())
            .authorId(authorId)
            .build());
    return ResponseEntity.noContent().build();
  }
}
