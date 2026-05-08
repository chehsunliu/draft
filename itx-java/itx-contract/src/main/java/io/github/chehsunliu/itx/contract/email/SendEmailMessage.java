package io.github.chehsunliu.itx.contract.email;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SendEmailMessage {
  String to;
  String subject;
  String body;
}
