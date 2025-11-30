package com.asledgehammer.rosetta.java;

import com.asledgehammer.rosetta.exception.ReadOnlyException;
import org.jetbrains.annotations.NotNull;

public class JavaDiscoverySettings {
  @NotNull private SuperPolicy superPolicy = SuperPolicy.IGNORE;
  private boolean exposeMethods = true;
  private boolean exposeFields = true;
  private boolean exposeConstructors = true;
  private boolean readOnly = false;

  public JavaDiscoverySettings() {}

  @NotNull
  public JavaDiscoverySettings setSuperPolicy(@NotNull SuperPolicy superPolicy) {
    checkReadOnlyStatus();
    this.superPolicy = superPolicy;
    return this;
  }

  @NotNull
  public JavaDiscoverySettings setExposeFields(boolean exposeFields) {
    checkReadOnlyStatus();
    this.exposeFields = exposeFields;
    return this;
  }

  @NotNull
  public JavaDiscoverySettings setExposeConstructors(boolean exposeConstructors) {
    checkReadOnlyStatus();
    this.exposeConstructors = exposeConstructors;
    return this;
  }

  @NotNull
  public JavaDiscoverySettings setExposeMethods(boolean exposeMethods) {
    checkReadOnlyStatus();
    this.exposeMethods = exposeMethods;
    return this;
  }

  @NotNull
  public JavaDiscoverySettings build() {
    checkReadOnlyStatus();
    this.readOnly = true;
    return this;
  }

  private void checkReadOnlyStatus() {
    if (readOnly) {
      throw new ReadOnlyException("The ExposureSettings is already built.");
    }
  }

  @NotNull
  public SuperPolicy getSuperPolicy() {
    return superPolicy;
  }

  public boolean exposeFields() {
    return exposeFields;
  }

  public boolean exposeConstructors() {
    return exposeConstructors;
  }

  public boolean exposeMethods() {
    return exposeMethods;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public enum SuperPolicy {
    EXPOSE,
    IGNORE
  }
}
