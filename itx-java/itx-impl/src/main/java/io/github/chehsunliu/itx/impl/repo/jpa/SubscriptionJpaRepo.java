package io.github.chehsunliu.itx.impl.repo.jpa;

import io.github.chehsunliu.itx.impl.repo.entity.SubscriptionEntity;
import io.github.chehsunliu.itx.impl.repo.entity.SubscriptionId;
import io.github.chehsunliu.itx.impl.repo.entity.UserEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionJpaRepo extends JpaRepository<SubscriptionEntity, SubscriptionId> {

  @Query(
      "SELECT u FROM SubscriptionEntity s JOIN UserEntity u ON u.id = s.id.authorId "
          + "WHERE s.id.subscriberId = :subscriberId "
          + "ORDER BY s.createdAt DESC, u.id ASC")
  List<UserEntity> listAuthorsBySubscriberId(@Param("subscriberId") UUID subscriberId);

  @Query(
      "SELECT u FROM SubscriptionEntity s JOIN UserEntity u ON u.id = s.id.subscriberId "
          + "WHERE s.id.authorId = :authorId "
          + "ORDER BY s.createdAt DESC, u.id ASC")
  List<UserEntity> listSubscribersByAuthorId(@Param("authorId") UUID authorId);
}
