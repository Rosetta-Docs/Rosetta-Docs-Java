package com.asledgehammer.rosetta.exception;

import org.jetbrains.annotations.NotNull;

public class DiscoveryException extends RosettaException {
  public DiscoveryException(@NotNull String message) {
    super(message);
  }

  public DiscoveryException(@NotNull String message, @NotNull Throwable cause) {
    super(message, cause);
  }
}
