package io.github.chehsunliu.itx.impl.email;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("itx.email")
public class EmailProperties {
  private String url;
  private String apiKey;
}
