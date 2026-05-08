package io.github.chehsunliu.itx.impl.repo.postgres;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.chehsunliu.itx.contract.repo.PostRepo;
import io.github.chehsunliu.itx.contract.repo.RepoFactory;
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public class PostgresRepoFactory implements RepoFactory {

  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;

  public PostgresRepoFactory(DataSource dataSource) {
    this.jdbc = new JdbcTemplate(dataSource);
    this.tx =
        new TransactionTemplate(
            new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource));
  }

  /** Build a HikariCP-backed DataSource from the {@code ITX_POSTGRES_*} env vars. */
  public static DataSource dataSourceFromEnv() {
    String host = required("ITX_POSTGRES_HOST");
    int port = Integer.parseInt(required("ITX_POSTGRES_PORT"));
    String dbName = required("ITX_POSTGRES_DB_NAME");
    String user = required("ITX_POSTGRES_USER");
    String password = required("ITX_POSTGRES_PASSWORD");

    HikariConfig cfg = new HikariConfig();
    cfg.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + dbName);
    cfg.setUsername(user);
    cfg.setPassword(password);
    cfg.setMaximumPoolSize(10);
    cfg.setDriverClassName("org.postgresql.Driver");
    return new HikariDataSource(cfg);
  }

  private static String required(String name) {
    String v = System.getenv(name);
    if (v == null || v.isBlank()) {
      throw new IllegalStateException("missing env var: " + name);
    }
    return v;
  }

  @Override
  public PostRepo createPostRepo() {
    return new PostgresPostRepo(jdbc, tx);
  }

  @Override
  public UserRepo createUserRepo() {
    return new PostgresUserRepo(jdbc);
  }

  @Override
  public SubscriptionRepo createSubscriptionRepo() {
    return new PostgresSubscriptionRepo(jdbc);
  }
}
