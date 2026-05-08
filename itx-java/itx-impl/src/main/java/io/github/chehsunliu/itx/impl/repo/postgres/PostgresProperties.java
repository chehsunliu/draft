package io.github.chehsunliu.itx.impl.repo.postgres;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("itx.postgres")
public class PostgresProperties {
  private String host = "localhost";
  private int port = 5432;
  private String dbName;
  private String user;
  private String password;
  private int maximumPoolSize = 10;
}
