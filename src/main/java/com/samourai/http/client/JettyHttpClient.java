package com.samourai.http.client;

import com.samourai.wallet.api.backend.beans.HttpException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.ProxyConfiguration;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyHttpClient extends JacksonHttpClient {
  protected static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String CONTENTTYPE_APPLICATION_JSON = "application/json";

  private HttpClient httpClient;
  private long requestTimeout;

  public JettyHttpClient(
      long requestTimeout, Optional<HttpProxy> cliProxyOptional, String userAgent) {
    super();
    this.requestTimeout = requestTimeout;
    this.httpClient = computeJettyClient(cliProxyOptional, userAgent);
  }

  private static HttpClient computeJettyClient(
      Optional<HttpProxy> cliProxyOptional, String userAgent) {
    // we use jetty for proxy SOCKS support
    HttpClient jettyHttpClient = new HttpClient();
    // jettyHttpClient.setSocketAddressResolver(new MySocketAddressResolver());

    // prevent user-agent tracking
    jettyHttpClient.setUserAgentField(new HttpField(HttpHeader.USER_AGENT, userAgent));

    // proxy
    if (cliProxyOptional != null && cliProxyOptional.isPresent()) {
      HttpProxy cliProxy = cliProxyOptional.get();
      if (log.isDebugEnabled()) {
        log.debug("+httpClient: proxy=" + cliProxy);
      }
      ProxyConfiguration.Proxy jettyProxy = cliProxy.computeJettyProxy();
      jettyHttpClient.getProxyConfiguration().getProxies().add(jettyProxy);
    } else {
      if (log.isDebugEnabled()) {
        log.debug("+httpClient: no proxy");
      }
    }
    return jettyHttpClient;
  }

  @Override
  public void connect() throws Exception {
    if (!httpClient.isRunning()) {
      httpClient.start();
    }
  }

  public void restart() {
    try {
      if (log.isDebugEnabled()) {
        log.debug("restart");
      }
      if (httpClient.isRunning()) {
        httpClient.stop();
      }
      httpClient.start();
    } catch (Exception e) {
      log.error("", e);
    }
  }

  @Override
  protected String requestJsonGet(String urlStr, Map<String, String> headers, boolean async)
      throws Exception {
    Request req = computeHttpRequest(urlStr, HttpMethod.GET, headers);
    return requestJson(req, async);
  }

  @Override
  protected String requestJsonPost(String urlStr, Map<String, String> headers, String jsonBody)
      throws Exception {
    Request req = computeHttpRequest(urlStr, HttpMethod.POST, headers);
    req.content(
        new StringContentProvider(CONTENTTYPE_APPLICATION_JSON, jsonBody, StandardCharsets.UTF_8));
    return requestJson(req, false);
  }

  @Override
  protected String requestJsonPostUrlEncoded(
      String urlStr, Map<String, String> headers, Map<String, String> body) throws Exception {
    Request req = computeHttpRequest(urlStr, HttpMethod.POST, headers);
    req.content(new FormContentProvider(computeBodyFields(body)));
    return requestJson(req, false);
  }

  private Fields computeBodyFields(Map<String, String> body) {
    Fields fields = new Fields();
    for (Map.Entry<String, String> entry : body.entrySet()) {
      fields.put(entry.getKey(), entry.getValue());
    }
    return fields;
  }

  private String requestJson(Request req, boolean async) throws Exception {
    String responseContent;
    if (async) {
      InputStreamResponseListener listener = new InputStreamResponseListener();
      req.send(listener);

      // Call to the listener's get() blocks until the headers arrived
      Response response = listener.get(requestTimeout, TimeUnit.MILLISECONDS);

      // Read content
      InputStream is = listener.getInputStream();
      Scanner s = new Scanner(is).useDelimiter("\\A");
      responseContent = s.hasNext() ? s.next() : null;

      // check status
      checkResponseStatus(response.getStatus(), responseContent);
    } else {
      ContentResponse response = req.send();
      checkResponseStatus(response.getStatus(), response.getContentAsString());
      responseContent = response.getContentAsString();
    }
    return responseContent;
  }

  private void checkResponseStatus(int status, String responseBody) throws HttpException {
    if (status != HttpStatus.OK_200) {
      log.error("Http query failed: status=" + status + ", responseBody=" + responseBody);
      throw new HttpException(new Exception("Http query failed: status=" + status), responseBody);
    }
  }

  public HttpClient getJettyHttpClient() throws Exception {
    connect();
    return httpClient;
  }

  private Request computeHttpRequest(String url, HttpMethod method, Map<String, String> headers)
      throws Exception {
    if (log.isDebugEnabled()) {
      String headersStr = headers != null ? " (" + headers.keySet() + ")" : "";
      log.debug("+" + method + ": " + url + headersStr);
    }
    Request req = getJettyHttpClient().newRequest(url);
    req.method(method);
    if (headers != null) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        req.header(entry.getKey(), entry.getValue());
      }
    }
    req.timeout(requestTimeout, TimeUnit.MILLISECONDS);
    return req;
  }

  @Override
  protected void onRequestError(Exception e) {
    super.onRequestError(e);
    restart();
  }
}
