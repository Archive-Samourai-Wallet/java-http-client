package com.samourai.http.client;

import com.samourai.wallet.httpClient.HttpProxy;
import com.samourai.wallet.httpClient.HttpUsage;
import com.samourai.wallet.httpClient.IHttpClientService;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyHttpClientService implements IHttpClientService {
  public static final String DEFAULT_USER_AGENT = "whirlpool-client";
  public static final long DEFAULT_TIMEOUT = 30000;
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private IHttpProxySupplier httpProxySupplier;
  private long requestTimeout;
  private String userAgent;
  private Map<HttpUsage, JettyHttpClient> httpClients;

  public JettyHttpClientService(
      long requestTimeout, String userAgent, IHttpProxySupplier httpProxySupplier) {
    this.httpProxySupplier =
        httpProxySupplier != null ? httpProxySupplier : computeHttpProxySupplierDefault();
    this.requestTimeout = requestTimeout;
    this.userAgent = userAgent;
    this.httpClients = new ConcurrentHashMap<>();
  }

  public JettyHttpClientService(long requestTimeout, String userAgent) {
    this(requestTimeout, userAgent, null);
  }

  public JettyHttpClientService(long requestTimeout) {
    this(requestTimeout, DEFAULT_USER_AGENT);
  }

  public JettyHttpClientService() {
    this(DEFAULT_TIMEOUT);
  }

  protected static IHttpProxySupplier computeHttpProxySupplierDefault() {
    return new IHttpProxySupplier() {
      @Override
      public Optional<HttpProxy> getHttpProxy(HttpUsage httpUsage) {
        return Optional.empty();
      }

      @Override
      public void changeIdentity() {}
    };
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
    Optional<HttpProxy> httpProxy = httpProxySupplier.getHttpProxy(httpUsage);
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
    httpProxySupplier.changeIdentity();
  }

  public IHttpProxySupplier getHttpProxySupplier() {
    return httpProxySupplier;
  }
}