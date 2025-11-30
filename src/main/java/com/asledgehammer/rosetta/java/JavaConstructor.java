package com.asledgehammer.rosetta.java;

import java.lang.reflect.Constructor;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class JavaConstructor extends JavaExecutable<Constructor<?>> {

  JavaConstructor(@NotNull Constructor<?> constructor) {
    super(constructor);
  }

  JavaConstructor(@NotNull String name, @NotNull Map<String, Object> raw) {
    super(name, raw);
  }

  @Override
  public String toString() {
    return "JavaConstructor \"" + getSignature() + "\"";
  }
}
