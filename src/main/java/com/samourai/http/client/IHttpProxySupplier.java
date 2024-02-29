package com.samourai.http.client;

import com.samourai.wallet.httpClient.HttpProxy;
import com.samourai.wallet.httpClient.HttpUsage;
import java.util.Optional;

public interface IHttpProxySupplier {
  Optional<HttpProxy> getHttpProxy(HttpUsage httpUsage);

  void changeIdentity();
}
