package io.github.chehsunliu.itx.impl.repo.mariadb;

import io.github.chehsunliu.itx.contract.repo.RepoNotFoundException;
import io.github.chehsunliu.itx.contract.repo.User;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@RequiredArgsConstructor
class MariaDbUserRepo implements UserRepo {

  private static final RowMapper<User> ROW_MAPPER =
      (rs, i) ->
          User.builder()
              .id(UUID.fromString(rs.getString("id")))
              .email(rs.getString("email"))
              .build();

  private final JdbcTemplate jdbc;

  @Override
  public User upsert(UpsertParams params) {
    jdbc.update(
        "INSERT INTO users (id, email) VALUES (?, ?) ON DUPLICATE KEY UPDATE id = id",
        params.getId().toString(),
        params.getEmail());
    return jdbc.queryForObject(
        "SELECT id, email FROM users WHERE id = ?", ROW_MAPPER, params.getId().toString());
  }

  @Override
  public User get(UUID id) {
    List<User> rows =
        jdbc.query("SELECT id, email FROM users WHERE id = ?", ROW_MAPPER, id.toString());
    if (rows.isEmpty()) {
      throw new RepoNotFoundException();
    }
    return rows.getFirst();
  }
}
