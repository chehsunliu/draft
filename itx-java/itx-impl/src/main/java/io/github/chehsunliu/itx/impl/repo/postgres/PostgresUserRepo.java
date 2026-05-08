package io.github.chehsunliu.itx.impl.repo.postgres;

import io.github.chehsunliu.itx.contract.repo.RepoNotFoundException;
import io.github.chehsunliu.itx.contract.repo.User;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@RequiredArgsConstructor
class PostgresUserRepo implements UserRepo {

  private static final RowMapper<User> ROW_MAPPER =
      (rs, i) -> User.builder().id((UUID) rs.getObject("id")).email(rs.getString("email")).build();

  private final JdbcTemplate jdbc;

  @Override
  public User upsert(UpsertParams params) {
    return jdbc.queryForObject(
        "INSERT INTO users (id, email) VALUES (?, ?) "
            + "ON CONFLICT (id) DO UPDATE SET id = EXCLUDED.id "
            + "RETURNING id, email",
        ROW_MAPPER,
        params.getId(),
        params.getEmail());
  }

  @Override
  public User get(UUID id) {
    List<User> rows = jdbc.query("SELECT id, email FROM users WHERE id = ?", ROW_MAPPER, id);
    if (rows.isEmpty()) {
      throw new RepoNotFoundException();
    }
    return rows.getFirst();
  }
}
