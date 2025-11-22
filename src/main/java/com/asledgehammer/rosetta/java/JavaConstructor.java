package com.asledgehammer.rosetta.java;

import java.lang.reflect.Constructor;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaConstructor extends JavaExecutable<Constructor<?>> {

  JavaConstructor(@NotNull Constructor<?> constructor) {
    super(constructor);
    //    System.out.println("new JavaConstructor(constructor = " + constructor + ")");
  }

  JavaConstructor(@NotNull String name, @NotNull Map<String, Object> raw) {
    super(name, raw);
    //    System.out.println("new JavaConstructor(name = " + name + ", raw = " + raw + ")");
  }

  @Override
  public String toString() {
    return "JavaConstructor \"" + getSignature() + "\"";
  }
}
