package io.github.chehsunliu.itx.impl.repo.postgres;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "itx.db.provider", havingValue = "postgres")
@RequiredArgsConstructor
public class PostgresDataSourceConfig {

  private final PostgresProperties props;

  @Bean
  DataSource dataSource() {
    HikariConfig cfg = new HikariConfig();
    cfg.setJdbcUrl(
        "jdbc:postgresql://" + props.getHost() + ":" + props.getPort() + "/" + props.getDbName());
    cfg.setUsername(props.getUser());
    cfg.setPassword(props.getPassword());
    cfg.setDriverClassName("org.postgresql.Driver");
    cfg.setMaximumPoolSize(props.getMaximumPoolSize());
    return new HikariDataSource(cfg);
  }
}
