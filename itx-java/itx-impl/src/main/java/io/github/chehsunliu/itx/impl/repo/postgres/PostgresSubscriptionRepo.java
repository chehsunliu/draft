package io.github.chehsunliu.itx.impl.repo.postgres;

import static io.github.chehsunliu.itx.impl.repo.Jdbc.bindAll;
import static io.github.chehsunliu.itx.impl.repo.Jdbc.mapAll;
import static io.github.chehsunliu.itx.impl.repo.Jdbc.useConnection;
import static io.github.chehsunliu.itx.impl.repo.Jdbc.useConnectionVoid;

import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.User;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class PostgresSubscriptionRepo implements SubscriptionRepo {
  private final DataSource ds;

  @Override
  public void subscribe(SubscribeParams params) {
    useConnectionVoid(
        ds,
        conn -> {
          try (var ps =
              conn.prepareStatement(
                  """
                  INSERT INTO subscriptions (subscriber_id, author_id) VALUES (?, ?)
                  ON CONFLICT (subscriber_id, author_id) DO NOTHING
                  """)) {
            bindAll(ps, params.subscriberId(), params.authorId());
            ps.executeUpdate();
          }
        });
  }

  @Override
  public void unsubscribe(UnsubscribeParams params) {
    useConnectionVoid(
        ds,
        conn -> {
          try (var ps =
              conn.prepareStatement(
                  "DELETE FROM subscriptions WHERE subscriber_id = ? AND author_id = ?")) {
            bindAll(ps, params.subscriberId(), params.authorId());
            ps.executeUpdate();
          }
        });
  }

  @Override
  public List<User> listAuthors(UUID subscriberId) {
    return useConnection(
        ds,
        conn -> {
          try (var ps =
              conn.prepareStatement(
                  """
                  SELECT u.id, u.email
                  FROM subscriptions s JOIN users u ON u.id = s.author_id
                  WHERE s.subscriber_id = ?
                  ORDER BY s.created_at DESC, u.id ASC
                  """)) {
            bindAll(ps, subscriberId);
            try (var rs = ps.executeQuery()) {
              return mapAll(rs, r -> new User(r.getObject("id", UUID.class), r.getString("email")));
            }
          }
        });
  }

  @Override
  public List<User> listSubscribers(UUID authorId) {
    return useConnection(
        ds,
        conn -> {
          try (var ps =
              conn.prepareStatement(
                  """
                  SELECT u.id, u.email
                  FROM subscriptions s JOIN users u ON u.id = s.subscriber_id
                  WHERE s.author_id = ?
                  ORDER BY s.created_at DESC, u.id ASC
                  """)) {
            bindAll(ps, authorId);
            try (var rs = ps.executeQuery()) {
              return mapAll(rs, r -> new User(r.getObject("id", UUID.class), r.getString("email")));
            }
          }
        });
  }
}
