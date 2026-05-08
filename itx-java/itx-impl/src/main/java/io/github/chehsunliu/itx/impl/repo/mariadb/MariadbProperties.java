package io.github.chehsunliu.itx.impl.repo.mariadb;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("itx.mariadb")
public class MariadbProperties {
  private String host = "localhost";
  private int port = 3306;
  private String dbName;
  private String user;
  private String password;
  private int maximumPoolSize = 10;
}
