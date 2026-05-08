package io.github.chehsunliu.itx.impl.queue;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("itx.queue")
public class QueueProperties {
  private String provider = "sqs";
}
