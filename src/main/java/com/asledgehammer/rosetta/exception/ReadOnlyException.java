package com.asledgehammer.rosetta.exception;

import org.jetbrains.annotations.NotNull;

public class ReadOnlyException extends RosettaException {

  public ReadOnlyException(@NotNull String message) {
    super(message);
  }

  public ReadOnlyException(@NotNull String message, @NotNull Throwable cause) {
    super(message, cause);
  }
}
