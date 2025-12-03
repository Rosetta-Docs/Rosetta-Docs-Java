package com.asledgehammer.rosetta.java;

import org.jetbrains.annotations.NotNull;

public class JavaSerializeInstance {
  @NotNull private final JavaSerializeSettings settings;
  @NotNull private final String id;

  private JavaTypeDictionary typeDictionary;

  JavaSerializeInstance(@NotNull JavaSerializeSettings settings, @NotNull String id) {

    this.id = id;
    this.settings = settings;

    final TypeDictionaryMode typeMode = settings.getTypeMode();
    if (typeMode != TypeDictionaryMode.NONE) {
      this.typeDictionary = new JavaTypeDictionary(typeMode, id);
    }
  }

  @NotNull
  public TypeDictionaryMode getTypeMode() {
    return settings.getTypeMode();
  }

  public boolean shouldNextPackages() {
    return settings.shouldNestPackages();
  }

  @NotNull
  public JavaTypeDictionary getTypeDictionary() {
    if (!hasTypeDictionary()) {
      throw new RuntimeException("TypeDictionary is null.");
    }
    return this.typeDictionary;
  }

  @NotNull
  public JavaSerializeSettings getSettings() {
    return this.settings;
  }

  @NotNull
  public String getId() {
    return this.id;
  }

  public boolean hasTypeDictionary() {
    return this.typeDictionary != null;
  }

  public boolean isWriteFullType() {
    return this.settings.isWriteFullType();
  }
}
