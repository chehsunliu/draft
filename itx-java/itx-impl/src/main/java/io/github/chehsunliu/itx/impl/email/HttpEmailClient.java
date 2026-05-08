package io.github.chehsunliu.itx.impl.email;

import static io.github.chehsunliu.itx.impl.Env.requireEnv;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chehsunliu.itx.contract.email.EmailClient;
import io.github.chehsunliu.itx.contract.email.EmailException;
import io.github.chehsunliu.itx.contract.email.SendEmailMessage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class HttpEmailClient implements EmailClient {
  private final URI url;
  private final String apiKey;
  private final HttpClient client;
  private final ObjectMapper mapper;

  public HttpEmailClient(String url, String apiKey) {
    this(url, apiKey, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
  }

  public HttpEmailClient(String url, String apiKey, HttpClient client) {
    this.url = URI.create(url);
    this.apiKey = apiKey;
    this.client = client;
    this.mapper = new ObjectMapper();
  }

  public static HttpEmailClient fromEnv() {
    return new HttpEmailClient(requireEnv("ITX_EMAIL_URL"), requireEnv("ITX_EMAIL_API_KEY"));
  }

  @Override
  public void send(SendEmailMessage msg) {
    byte[] payload;
    try {
      payload = mapper.writeValueAsBytes(msg);
    } catch (Exception e) {
      throw new EmailException("failed to serialize email payload", e);
    }
    HttpRequest req =
        HttpRequest.newBuilder(url)
            .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .build();
    HttpResponse<String> resp;
    try {
      resp = client.send(req, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      throw new EmailException("email API call failed", e);
    }
    int status = resp.statusCode();
    if (status / 100 != 2) {
      throw new EmailException("email API returned " + status + ": " + resp.body());
    }
  }
}
