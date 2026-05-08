package io.github.chehsunliu.itx.impl.queue.rabbitmq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("itx.rabbitmq")
public class RabbitProperties {
  private String host;
  private int port = 5672;
  private String user;
  private String password;
  private int maxConcurrency = 100;
  private String controlStandardQueue;
  private String controlPremiumQueue;
  private String computeStandardQueue;
  private String computePremiumQueue;
}
