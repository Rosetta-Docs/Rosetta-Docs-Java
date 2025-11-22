package com.asledgehammer.rosetta.exception;

import com.asledgehammer.rosetta.java.JavaClass;
import org.jetbrains.annotations.NotNull;

public class ClassAlreadyDiscoveredException extends RuntimeException {
  private final JavaClass container;

  public ClassAlreadyDiscoveredException(@NotNull JavaClass container) {
    super("The class is already discovered: " + container.getReflectionTarget().getName());
    this.container = container;
  }

  @NotNull
  public JavaClass getContainer() {
    return container;
  }
}
