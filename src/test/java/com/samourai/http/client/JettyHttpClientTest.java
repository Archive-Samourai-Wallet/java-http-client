package com.samourai.http.client;

import com.samourai.wallet.httpClient.HttpUsage;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JettyHttpClientTest {
  private JettyHttpClient httpClient;

  public JettyHttpClientTest() {
    JettyHttpClientService jettyHttpClientService = new JettyHttpClientService(30000);
    httpClient = jettyHttpClientService.getHttpClient(HttpUsage.BACKEND);
  }

  @Test
  public void requestJsonGet() throws Exception {
    String response = httpClient.requestJsonGet("https://eu.httpbin.org/get", null, false);
    Assertions.assertTrue(response.contains("User-Agent"));
  }

  @Test
  public void requestJsonPost() throws Exception {
    String response =
        httpClient.requestJsonPost("https://eu.httpbin.org/post", null, "{foo:'bar'}");
    Assertions.assertTrue(response.contains("{foo:'bar'}"));
  }

  @Test
  public void requestStringPost() throws Exception {
    String response =
        httpClient.requestStringPost(
            "https://eu.httpbin.org/post",
            null,
            JettyHttpClient.CONTENTTYPE_APPLICATION_JSON,
            "foo=bar");
    Assertions.assertTrue(response.contains("foo=bar"));
  }

  @Test
  public void requestJsonPostUrlEncoded() throws Exception {
    Map<String, String> body = new LinkedHashMap<>();
    body.put("foo", "bar");
    String response =
        httpClient.requestJsonPostUrlEncoded("https://eu.httpbin.org/post", null, body);
    Assertions.assertTrue(response.contains("\"foo\": \"bar\""));
  }
}
