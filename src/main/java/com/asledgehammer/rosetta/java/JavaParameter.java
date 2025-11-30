package com.asledgehammer.rosetta.java;

import com.asledgehammer.rosetta.NamedEntity;
import com.asledgehammer.rosetta.Notable;
import com.asledgehammer.rosetta.RosettaObject;
import com.asledgehammer.rosetta.exception.MissingKeyException;
import com.asledgehammer.rosetta.exception.ValueTypeException;
import com.asledgehammer.rosetta.java.reference.ClassReference;
import com.asledgehammer.rosetta.java.reference.TypeReference;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaParameter extends RosettaObject
    implements JavaTyped, NamedEntity, Notable, Reflected<Parameter> {

  /** Used to validate and check names assigned to parameters. */
  private static final Pattern REGEX_PARAM_NAME = Pattern.compile("^[a-zA-Z_$][a-zA-Z_$0-9]*$");

  private final Parameter target;
  private TypeReference type;

  /** The formal name of the parameter. */
  private String name;

  private String notes;

  private boolean nullable;

  public JavaParameter(@NotNull Parameter parameter) {
    super();
    this.target = parameter;
    this.name = parameter.getName();
    this.type = TypeReference.of(parameter.getParameterizedType());
    this.nullable = !this.type.isPrimitive();
  }

  public JavaParameter(@NotNull Map<String, Object> raw) {
    this.target = null;
    onLoad(raw);
  }

  protected void onLoad(@NotNull Map<String, Object> raw) {

    // Read the name.
    Object oName = raw.get("name");
    if (oName == null) {
      throw new MissingKeyException("field", "name");
    } else if (!(oName instanceof String)) {
      throw new ValueTypeException("field", "name", oName.getClass(), String.class);
    }
    this.name = (String) oName;

    // Resolve the type.
    if (!raw.containsKey("type")) {
      throw new MissingKeyException(name, "type");
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

    // TODO: Load attributes / conditions?
  }

  @NotNull
  protected Map<String, Object> onSave(
      @NotNull JavaSerializeInstance serialize,
      @NotNull ClassReference reference,
      @NotNull Class<?> deCl) {
    Map<String, Object> raw = new HashMap<>();
    raw.put("name", getName());
    if (hasNotes()) {
      raw.put("notes", getNotes());
    }
    raw.put("nullable", isNullable());
    if (serialize.hasTypeDictionary()) {
      raw.put("type", serialize.getTypeDictionary().register(serialize, this.type, reference, deCl));
    } else {
      raw.put("type", JavaLanguage.serializeType(serialize, this.type, reference, deCl));
    }
    return raw;
  }

  @NotNull
  @Override
  public Parameter getReflectionTarget() {
    return this.target;
  }

  @NotNull
  @Override
  public String getName() {
    return this.name;
  }

  /**
   * Sets the formal name for the parameter.
   *
   * @param name The name to set.
   * @throws NullPointerException Thrown if the name is null.
   * @throws IllegalArgumentException Thrown if the name is empty.
   */
  public void setName(@NotNull String name) {
    if (name.isEmpty()) {
      throw new IllegalArgumentException("The name is empty.");
    } else if (!isValidName(name)) {
      throw new IllegalArgumentException("The name is not valid: " + name);
    }
    if (Objects.equals(this.name, name)) return;

    this.name = name;
  }

  @NotNull
  @Override
  public TypeReference getType() {
    return this.type;
  }

  @Override
  public void setType(@NotNull TypeReference type) {
    this.type = type;
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
    this.notes = notes == null || notes.isEmpty() ? null : notes;
  }

  public boolean isNullable() {
    return this.nullable;
  }

  public void setNullable(boolean nullable) {
    this.nullable = nullable;
  }

  @Override
  public String toString() {
    return "JavaParameter \"" + getName() + "\"";
  }

  /**
   * Tests if a Java-parameter's name is valid.
   *
   * @param name The name to test.
   * @return True if the parameter's name is valid.
   * @throws NullPointerException Thrown if the name is null.
   * @throws IllegalArgumentException Thrown if the name is empty.
   */
  public static boolean isValidName(@NotNull String name) {
    return !name.isEmpty() && REGEX_PARAM_NAME.matcher(name).find();
  }
}
