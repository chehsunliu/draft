package io.github.chehsunliu.itx.impl.repo.mariadb;

import static io.github.chehsunliu.itx.impl.Env.requireEnv;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.chehsunliu.itx.contract.repo.PostRepo;
import io.github.chehsunliu.itx.contract.repo.RepoFactory;
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class MariaDbRepoFactory implements RepoFactory {
  private final DataSource dataSource;

  public static MariaDbRepoFactory fromEnv() {
    return new MariaDbRepoFactory(dataSourceFromEnv());
  }

  public static DataSource dataSourceFromEnv() {
    HikariConfig cfg = new HikariConfig();
    cfg.setJdbcUrl(
        "jdbc:mariadb://"
            + requireEnv("ITX_MARIADB_HOST")
            + ":"
            + requireEnv("ITX_MARIADB_PORT")
            + "/"
            + requireEnv("ITX_MARIADB_DB_NAME")
            + "?useSsl=false");
    cfg.setUsername(requireEnv("ITX_MARIADB_USER"));
    cfg.setPassword(requireEnv("ITX_MARIADB_PASSWORD"));
    cfg.setDriverClassName("org.mariadb.jdbc.Driver");
    cfg.setMaximumPoolSize(10);
    return new HikariDataSource(cfg);
  }

  @Override
  public PostRepo createPostRepo() {
    return new MariaDbPostRepo(dataSource);
  }

  @Override
  public UserRepo createUserRepo() {
    return new MariaDbUserRepo(dataSource);
  }

  @Override
  public SubscriptionRepo createSubscriptionRepo() {
    return new MariaDbSubscriptionRepo(dataSource);
  }
}
