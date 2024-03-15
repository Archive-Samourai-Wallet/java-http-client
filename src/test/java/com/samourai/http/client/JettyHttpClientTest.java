package com.samourai.http.client;

import com.samourai.wallet.httpClient.HttpResponseException;
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
    Assertions.assertTrue(response.contains("get"));
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

  @Test
  public void requestJsonPostUrlEncodedFailure() throws Exception {
    String hex =
        "010000000001015f5e263ccc20a0d6b34d9b26eb8ac14ff7623b68dd55f8b2fb35d02e49cc9fcf0000000000ffffffff050000000000000000536a4c506ccfebfe774392f9b35bd2440e36370f1f904b7b0bab5bc4437b04748f02fa705bb14939be6d926a66fa65983ade03633d1fdb8c858e44f83ce8d625d5b27cc445663e665ecfd20d811a06061baad401881300000000000016001484a3fbbfdbd4166e5835e99b6ca7cd112c3c9c5559370000000000001600142c0038638b6ee7cd9a11653e1cf3a8fccf249fb8a687010000000000160014022ffa36b5b8dc9ee0a8ca4e6b51ec1745a70df0a68701000000000016001423d282ec3309b6de0b3f76f642150dbd54a8d4d402483045022100f48c654b60a90cc7298590da04e1bc63f90431554c73408064bcc1c5906b29d902205bdc9e3b081136aa1d9d56d3105f75a6308f5c2b38ac3126fbb98c14bc1b392a012103900b856c3780555390f1e70a68bdd3e341efc0110db071bdf190b76fe041657900000000";
    Map<String, String> body = new LinkedHashMap<>();
    body.put("tx", hex);
    HttpResponseException e =
        Assertions.assertThrows(
            HttpResponseException.class,
            () -> {
              httpClient.requestJsonPostUrlEncoded(
                  "https://api.samouraiwallet.com/test/v2/pushtx/", null, body);
            });
    Assertions.assertEquals(400, e.getStatusCode());
    Assertions.assertEquals(
        "{\n"
            + "  \"status\": \"error\",\n"
            + "  \"error\": \"bad-txns-inputs-missingorspent\"\n"
            + "}",
        e.getResponseBody());
  }
}
