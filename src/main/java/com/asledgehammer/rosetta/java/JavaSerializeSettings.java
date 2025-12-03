package com.asledgehammer.rosetta.java;

import com.asledgehammer.rosetta.RosettaSerializeSettings;
import com.asledgehammer.rosetta.exception.ReadOnlyException;
import org.jetbrains.annotations.NotNull;

public class JavaSerializeSettings implements RosettaSerializeSettings {

  private boolean readOnly = false;

  private FullTypeRenderSettings fullTypeRenderSettings =
      FullTypeRenderSettings.ONLY_WITH_MODIFIERS;

  private TypeDictionaryMode typeMode = TypeDictionaryMode.NONE;

  private boolean writeFullType = false;

  private boolean nestPackages = false;

  public boolean isWriteFullType() {
    return writeFullType;
  }

  @NotNull
  public JavaSerializeSettings setWriteFullType(boolean writeFullType) {
    checkReadOnlyStatus();
    this.writeFullType = writeFullType;
    return this;
  }

  public FullTypeRenderSettings getFullTypeRenderSettings() {
    return fullTypeRenderSettings;
  }

  @NotNull
  public JavaSerializeSettings setFullTypeRenderSettings(
      @NotNull FullTypeRenderSettings fullTypeRenderSettings) {
    checkReadOnlyStatus();
    this.fullTypeRenderSettings = fullTypeRenderSettings;
    return this;
  }

  public boolean shouldNestPackages() {
    return nestPackages;
  }

  @NotNull
  public JavaSerializeSettings setNestPackages(boolean flag) {
    checkReadOnlyStatus();
    this.nestPackages = flag;
    return this;
  }

  public TypeDictionaryMode getTypeMode() {
    return typeMode;
  }

  @NotNull
  public JavaSerializeSettings setTypeMode(TypeDictionaryMode typeMode) {
    checkReadOnlyStatus();
    this.typeMode = typeMode;
    return this;
  }

  public JavaSerializeSettings build() {
    this.readOnly = true;
    return this;
  }

  private void checkReadOnlyStatus() {
    if (this.isReadOnly()) {
      throw new ReadOnlyException("The JavaSerializeSettings is read-only.");
    }
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public enum FullTypeRenderSettings {
    ALWAYS,
    NEVER,
    ONLY_WITH_MODIFIERS
  }
}
