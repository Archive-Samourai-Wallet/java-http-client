package com.samourai.http.client;

import com.samourai.wallet.httpClient.HttpProxy;
import com.samourai.wallet.httpClient.HttpUsage;
import com.samourai.wallet.httpClient.IHttpClientService;
import com.samourai.wallet.httpClient.IHttpProxyService;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyHttpClientService implements IHttpClientService {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private IHttpProxyService httpProxyService;
  private long requestTimeout;
  private String userAgent;
  private Map<HttpUsage, JettyHttpClient> httpClients;

  public JettyHttpClientService(
      IHttpProxyService httpProxyService, long requestTimeout, String userAgent) {
    this.httpProxyService = httpProxyService;
    this.requestTimeout = requestTimeout;
    this.userAgent = userAgent;
    this.httpClients = new ConcurrentHashMap<>();
  }

  @Override
  public JettyHttpClient getHttpClient(HttpUsage httpUsage) {
    JettyHttpClient httpClient = httpClients.get(httpUsage);
    if (httpClient == null) {
      if (log.isDebugEnabled()) {
        log.debug("+httpClient[" + httpUsage + "]");
      }
      httpClient = computeHttpClient(httpUsage);
      httpClients.put(httpUsage, httpClient);
    }
    return httpClient;
  }

  private JettyHttpClient computeHttpClient(HttpUsage httpUsage) {
    // use Tor proxy if any
    Optional<HttpProxy> httpProxy = httpProxyService.getHttpProxy(httpUsage);
    return new JettyHttpClient(requestTimeout, httpProxy, userAgent);
  }

  @Override
  public void stop() {
    for (JettyHttpClient httpClient : httpClients.values()) {
      httpClient.stop();
    }
  }

  @Override
  public void changeIdentity() {
    stop();
    httpProxyService.changeIdentity();
  }

  public IHttpProxyService getHttpProxyService() {
    return httpProxyService;
  }
}
