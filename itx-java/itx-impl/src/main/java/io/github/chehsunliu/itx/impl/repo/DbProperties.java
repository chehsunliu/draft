package io.github.chehsunliu.itx.impl.repo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("itx.db")
public class DbProperties {
  private String provider = "";
}
