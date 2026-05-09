package io.github.chehsunliu.itx.impl.repo.postgres;

import static io.github.chehsunliu.itx.impl.repo.Jdbc.bindAll;
import static io.github.chehsunliu.itx.impl.repo.Jdbc.firstOrNull;
import static io.github.chehsunliu.itx.impl.repo.Jdbc.useConnection;

import io.github.chehsunliu.itx.contract.repo.RepoNotFoundException;
import io.github.chehsunliu.itx.contract.repo.User;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class PostgresUserRepo implements UserRepo {
    private final DataSource ds;

    @Override
    public User upsert(UpsertParams params) {
        return useConnection(ds, conn -> {
            try (var ps = conn.prepareStatement("""
                  INSERT INTO users (id, email) VALUES (?, ?)
                  ON CONFLICT (id) DO UPDATE SET id = EXCLUDED.id
                  RETURNING id, email
                  """)) {
                bindAll(ps, params.id(), params.email());
                try (var rs = ps.executeQuery()) {
                    return firstOrNull(rs, r -> new User(r.getObject("id", UUID.class), r.getString("email")))
                            .orElseThrow(() -> new IllegalStateException("upsert returned no row"));
                }
            }
        });
    }

    @Override
    public User get(UUID id) {
        return useConnection(ds, conn -> {
            try (var ps = conn.prepareStatement("SELECT id, email FROM users WHERE id = ?")) {
                bindAll(ps, id);
                try (var rs = ps.executeQuery()) {
                    return firstOrNull(rs, r -> new User(r.getObject("id", UUID.class), r.getString("email")))
                            .orElseThrow(RepoNotFoundException::new);
                }
            }
        });
    }
}
