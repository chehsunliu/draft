package io.github.chehsunliu.itx.impl.queue.sqs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("itx.sqs")
public class SqsProperties {
  private String localEndpointUrl;
  private int maxConcurrency = 100;
  private String controlStandardQueueUrl;
  private String controlPremiumQueueUrl;
  private String computeStandardQueueUrl;
  private String computePremiumQueueUrl;
}
