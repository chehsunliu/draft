package io.github.chehsunliu.itx.impl.repo.mariadb;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "itx.db.provider", havingValue = "mariadb")
@RequiredArgsConstructor
public class MariadbDataSourceConfig {

  private final MariadbProperties props;

  @Bean
  DataSource dataSource() {
    HikariConfig cfg = new HikariConfig();
    cfg.setJdbcUrl(
        "jdbc:mariadb://" + props.getHost() + ":" + props.getPort() + "/" + props.getDbName());
    cfg.setUsername(props.getUser());
    cfg.setPassword(props.getPassword());
    cfg.setDriverClassName("org.mariadb.jdbc.Driver");
    cfg.setMaximumPoolSize(props.getMaximumPoolSize());
    return new HikariDataSource(cfg);
  }

  // MariaDB stores TIMESTAMP without offset, so normalize OffsetDateTime through UTC; UUID
  // columns are CHAR(36) on this schema so steer Hibernate to that JDBC type.
  @Bean
  HibernatePropertiesCustomizer mariadbHibernateCustomizer() {
    return (Map<String, Object> p) -> {
      p.put("hibernate.timezone.default_storage", "NORMALIZE_UTC");
      p.put("hibernate.type.preferred_uuid_jdbc_type", "CHAR");
    };
  }
}
