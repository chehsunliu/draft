package io.github.chehsunliu.itx.backend.middleware;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ItxContext {
  public static final String ATTR = "itx.context";

  public static final String HEADER_REQUEST_ID = "X-Itx-Request-Id";
  public static final String HEADER_USER_ID = "X-Itx-User-Id";
  public static final String HEADER_USER_EMAIL = "X-Itx-User-Email";

  UUID requestId;
  UUID userId;
  String userEmail;
}
