package com.asledgehammer.rosetta.java;

import org.jetbrains.annotations.NotNull;

public class JavaDeserializeInstance {

  private final JavaDeserializeSettings settings;

  public JavaDeserializeInstance(@NotNull JavaDeserializeSettings settings) {
    this.settings = settings;
  }

  @NotNull
  public JavaDeserializeSettings getSettings() {
    return settings;
  }
}
