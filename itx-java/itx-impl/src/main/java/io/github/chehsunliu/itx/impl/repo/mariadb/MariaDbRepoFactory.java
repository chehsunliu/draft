package io.github.chehsunliu.itx.impl.repo.mariadb;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.chehsunliu.itx.contract.repo.PostRepo;
import io.github.chehsunliu.itx.contract.repo.RepoFactory;
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public class MariaDbRepoFactory implements RepoFactory {

  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;

  public MariaDbRepoFactory(DataSource dataSource) {
    this.jdbc = new JdbcTemplate(dataSource);
    this.tx =
        new TransactionTemplate(
            new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource));
  }

  /** Build a HikariCP-backed DataSource from the {@code ITX_MARIADB_*} env vars. */
  public static DataSource dataSourceFromEnv() {
    String host = required("ITX_MARIADB_HOST");
    int port = Integer.parseInt(required("ITX_MARIADB_PORT"));
    String dbName = required("ITX_MARIADB_DB_NAME");
    String user = required("ITX_MARIADB_USER");
    String password = required("ITX_MARIADB_PASSWORD");

    HikariConfig cfg = new HikariConfig();
    cfg.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + dbName + "?useSsl=false");
    cfg.setUsername(user);
    cfg.setPassword(password);
    cfg.setMaximumPoolSize(10);
    cfg.setDriverClassName("org.mariadb.jdbc.Driver");
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
    return new MariaDbPostRepo(jdbc, tx);
  }

  @Override
  public UserRepo createUserRepo() {
    return new MariaDbUserRepo(jdbc);
  }

  @Override
  public SubscriptionRepo createSubscriptionRepo() {
    return new MariaDbSubscriptionRepo(jdbc);
  }
}
