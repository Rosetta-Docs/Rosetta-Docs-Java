package com.asledgehammer.rosetta.java;

import com.asledgehammer.rosetta.java.reference.ClassReference;
import com.asledgehammer.rosetta.java.reference.TypeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class JavaTypeParameter {

  private TypeReference type;
  private String notes;

  public JavaTypeParameter(@NotNull TypeReference type) {
    this.type = type;
  }

  public TypeReference getType() {
    return type;
  }

  public void setType(TypeReference type) {
    this.type = type;
  }

  @Nullable
  public String getNotes() {
    return this.notes;
  }

  public void setNotes(@Nullable String notes) {
    this.notes = notes;
  }

  @NotNull
  public Map<String, Object> onSave(@NotNull JavaSerializeInstance serialize, @NotNull ClassReference reference, @NotNull Class<?> deCl) {
    Map<String, Object> raw = new HashMap<>();

    raw.put("type", JavaLanguage.serializeType(type, reference, deCl));

    return raw;
  }
}
