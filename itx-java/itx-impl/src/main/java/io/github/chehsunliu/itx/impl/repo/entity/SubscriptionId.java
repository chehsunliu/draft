package io.github.chehsunliu.itx.impl.repo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionId implements Serializable {

  @Column(name = "subscriber_id", nullable = false)
  private UUID subscriberId;

  @Column(name = "author_id", nullable = false)
  private UUID authorId;

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SubscriptionId other)) return false;
    return Objects.equals(subscriberId, other.subscriberId)
        && Objects.equals(authorId, other.authorId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subscriberId, authorId);
  }
}
