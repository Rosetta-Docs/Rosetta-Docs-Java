package com.asledgehammer.rosetta.java;

import com.asledgehammer.rosetta.Notable;
import com.asledgehammer.rosetta.exception.MissingKeyException;
import com.asledgehammer.rosetta.exception.ValueTypeException;
import com.asledgehammer.rosetta.java.reference.ClassReference;
import com.asledgehammer.rosetta.java.reference.TypeReference;
import com.asledgehammer.rosetta.RosettaObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class JavaReturn extends RosettaObject implements Notable {

  private TypeReference type;

  private String notes;

  private boolean nullable;

  public JavaReturn(@NotNull Type type) {
    super();
    this.type = TypeReference.of(type);
    this.nullable = !this.type.isPrimitive();
  }

  public JavaReturn(@NotNull Map<String, Object> raw) {
    super();
    onLoad(raw);
  }

  protected void onLoad(@NotNull Map<String, Object> raw) {
    // Resolve the type.
    if (!raw.containsKey("type")) {
      throw new MissingKeyException("return[\"type\"]", "type");
    }
    this.type = JavaLanguage.resolveType(raw.get("type"));

    // If defined, set the nullable flag.
    if (raw.containsKey("nullable")) {
      Object oNullable = raw.get("nullable");
      if (!(oNullable instanceof Boolean)) {
        throw new ValueTypeException("parameter", "nullable", oNullable.getClass(), Boolean.class);
      }
      this.nullable = (boolean) (Boolean) oNullable;
    } else {
      this.nullable = !type.isPrimitive();
    }

    // Load notes. (If present)
    if (raw.containsKey("notes")) {
      this.notes = raw.get("notes").toString();
    }
  }

  @NotNull
  protected Map<String, Object> onSave(
      @NotNull JavaSerializeInstance serialize,
      @NotNull ClassReference reference,
      @NotNull Class<?> deCl) {
    final Map<String, Object> raw = new HashMap<>();

    if (hasNotes()) {
      raw.put("notes", getNotes());
    }

    if (isNullable()) {
      raw.put("nullable", true);
    }

    if (serialize.hasTypeDictionary()) {
      raw.put("type", serialize.getTypeDictionary().register(serialize, getType(), reference, deCl));
    } else {
      raw.put("type", JavaLanguage.serializeType(serialize, getType(), reference, deCl));
    }

    return raw;
  }

  @NotNull
  public TypeReference getType() {
    return this.type;
  }

  public void setType(@NotNull TypeReference type) {
    this.type = type;
    setDirty();
  }

  @Override
  public boolean hasNotes() {
    return this.notes != null && !this.notes.isEmpty();
  }

  @Override
  @NotNull
  public String getNotes() {
    if (!hasNotes()) {
      throw new NullPointerException("The object has no notes.");
    }
    return this.notes;
  }

  @Override
  public void setNotes(@Nullable String notes) {
    notes = notes == null || notes.isEmpty() ? null : notes;

    // Catch redundant changes to not set dirty flag.
    if (this.notes == null) {
      if (notes == null) return;
    } else if (this.notes.equals(notes)) return;

    this.notes = notes;

    setDirty();
  }

  public boolean isNullable() {
    return nullable;
  }

  public void setNullable(boolean nullable) {
    this.nullable = nullable;
  }

  /**
   * @return True if the returns definition should save, having either a non-void return or defined
   *     notes.
   */
  boolean shouldSave() {
    return !this.type.getBase().equals("void") || this.notes != null;
  }
}
