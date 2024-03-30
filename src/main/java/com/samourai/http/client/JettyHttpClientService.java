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
  public static final long DEFAULT_TIMEOUT = 30000;
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private IHttpProxySupplier httpProxySupplier;
  private long requestTimeout;
  protected Map<HttpUsage, JettyHttpClient> httpClients; // used by Sparrow

  public JettyHttpClientService(long requestTimeout, IHttpProxySupplier httpProxySupplier) {
    this.httpProxySupplier =
        httpProxySupplier != null ? httpProxySupplier : computeHttpProxySupplierDefault();
    this.requestTimeout = requestTimeout;
    this.httpClients = new ConcurrentHashMap<>();
  }

  public JettyHttpClientService(long requestTimeout) {
    this(requestTimeout, null);
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

  protected JettyHttpClient computeHttpClient(HttpUsage httpUsage) {
    return new JettyHttpClient(requestTimeout, httpProxySupplier, httpUsage);
  }

  @Override
  public synchronized void stop() {
    for (JettyHttpClient httpClient : httpClients.values()) {
      httpClient.stop();
    }
    httpClients.clear();
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
