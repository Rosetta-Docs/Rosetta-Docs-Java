package com.asledgehammer.rosetta.java;

import org.jetbrains.annotations.NotNull;

public enum TypeDictionaryMode {
  DICTIONARY("dictionary"),
  LIST("list"),
  NONE("none");

  private final @NotNull String id;

  TypeDictionaryMode(@NotNull String id) {
    this.id = id;
  }

  @NotNull
  public String getID() {
    return id;
  }

  @NotNull
  public static TypeDictionaryMode of(@NotNull String id) {
    return switch (id) {
      case "dictionary" -> DICTIONARY;
      case "list" -> LIST;
      case "none" -> NONE;
      default -> throw new IllegalArgumentException("Unknown TypeDictionaryMode ID: " + id);
    };
  }
}
