package io.github.chehsunliu.itx.impl.email;

import io.github.chehsunliu.itx.contract.email.EmailClient;
import io.github.chehsunliu.itx.contract.email.EmailException;
import io.github.chehsunliu.itx.contract.email.SendEmailMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

public class HttpEmailClient implements EmailClient {

  private final RestClient client;
  private final String url;
  private final String apiKey;

  public HttpEmailClient(String url, String apiKey) {
    this.url = url;
    this.apiKey = apiKey;
    this.client = RestClient.builder().build();
  }

  /** Build from {@code ITX_EMAIL_URL} and {@code ITX_EMAIL_API_KEY} env vars. */
  public static HttpEmailClient fromEnv() {
    return new HttpEmailClient(requireEnv("ITX_EMAIL_URL"), requireEnv("ITX_EMAIL_API_KEY"));
  }

  private static String requireEnv(String name) {
    String v = System.getenv(name);
    if (v == null || v.isBlank()) {
      throw new IllegalStateException("missing env var: " + name);
    }
    return v;
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
