package com.samourai.http.client;

import java.util.Optional;

public enum HttpProxyProtocol {
  HTTP,
  SOCKS;

  public static Optional<HttpProxyProtocol> find(String value) {
    try {
      return Optional.of(valueOf(value));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
