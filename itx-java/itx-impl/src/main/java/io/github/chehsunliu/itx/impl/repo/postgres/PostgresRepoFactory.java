package io.github.chehsunliu.itx.impl.repo.postgres;

import static io.github.chehsunliu.itx.impl.Env.requireEnv;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.chehsunliu.itx.contract.repo.PostRepo;
import io.github.chehsunliu.itx.contract.repo.RepoFactory;
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import javax.sql.DataSource;

public final class PostgresRepoFactory implements RepoFactory {
  private final DataSource dataSource;

  public PostgresRepoFactory(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public static PostgresRepoFactory fromEnv() {
    return new PostgresRepoFactory(dataSourceFromEnv());
  }

  public static DataSource dataSourceFromEnv() {
    HikariConfig cfg = new HikariConfig();
    cfg.setJdbcUrl(
        "jdbc:postgresql://"
            + requireEnv("ITX_POSTGRES_HOST")
            + ":"
            + requireEnv("ITX_POSTGRES_PORT")
            + "/"
            + requireEnv("ITX_POSTGRES_DB_NAME"));
    cfg.setUsername(requireEnv("ITX_POSTGRES_USER"));
    cfg.setPassword(requireEnv("ITX_POSTGRES_PASSWORD"));
    cfg.setDriverClassName("org.postgresql.Driver");
    cfg.setMaximumPoolSize(10);
    return new HikariDataSource(cfg);
  }

  @Override
  public PostRepo createPostRepo() {
    return new PostgresPostRepo(dataSource);
  }

  @Override
  public UserRepo createUserRepo() {
    return new PostgresUserRepo(dataSource);
  }

  @Override
  public SubscriptionRepo createSubscriptionRepo() {
    return new PostgresSubscriptionRepo(dataSource);
  }
}
