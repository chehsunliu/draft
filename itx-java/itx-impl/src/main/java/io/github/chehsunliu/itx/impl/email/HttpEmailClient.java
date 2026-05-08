package io.github.chehsunliu.itx.impl.email;

import io.github.chehsunliu.itx.contract.email.EmailClient;
import io.github.chehsunliu.itx.contract.email.EmailException;
import io.github.chehsunliu.itx.contract.email.SendEmailMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(name = "itx.email.url")
public class HttpEmailClient implements EmailClient {

  private final RestClient client;
  private final String url;
  private final String apiKey;

  public HttpEmailClient(EmailProperties props) {
    this.url = props.getUrl();
    this.apiKey = props.getApiKey();
    this.client = RestClient.builder().build();
  }

  @Override
  public void send(SendEmailMessage msg) {
    try {
      client
          .post()
          .uri(url)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
          .contentType(MediaType.APPLICATION_JSON)
          .body(msg)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException e) {
      throw new EmailException(
          "email API returned " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString(),
          e);
    } catch (RuntimeException e) {
      throw new EmailException("email API call failed", e);
    }
  }
}
