package io.github.chehsunliu.itx.impl.repo.mariadb;

import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.User;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@RequiredArgsConstructor
class MariaDbSubscriptionRepo implements SubscriptionRepo {

  private static final RowMapper<User> USER_MAPPER =
      (rs, i) ->
          User.builder()
              .id(UUID.fromString(rs.getString("id")))
              .email(rs.getString("email"))
              .build();

  private final JdbcTemplate jdbc;

  @Override
  public void subscribe(SubscribeParams params) {
    jdbc.update(
        "INSERT IGNORE INTO subscriptions (subscriber_id, author_id) VALUES (?, ?)",
        params.getSubscriberId().toString(),
        params.getAuthorId().toString());
  }

  @Override
  public void unsubscribe(UnsubscribeParams params) {
    jdbc.update(
        "DELETE FROM subscriptions WHERE subscriber_id = ? AND author_id = ?",
        params.getSubscriberId().toString(),
        params.getAuthorId().toString());
  }

  @Override
  public List<User> listAuthors(UUID subscriberId) {
    return jdbc.query(
        "SELECT u.id, u.email "
            + "FROM subscriptions s JOIN users u ON u.id = s.author_id "
            + "WHERE s.subscriber_id = ? "
            + "ORDER BY s.created_at DESC, u.id ASC",
        USER_MAPPER,
        subscriberId.toString());
  }

  @Override
  public List<User> listSubscribers(UUID authorId) {
    return jdbc.query(
        "SELECT u.id, u.email "
            + "FROM subscriptions s JOIN users u ON u.id = s.subscriber_id "
            + "WHERE s.author_id = ? "
            + "ORDER BY s.created_at DESC, u.id ASC",
        USER_MAPPER,
        authorId.toString());
  }
}
