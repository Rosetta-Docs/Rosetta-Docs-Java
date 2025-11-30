package com.asledgehammer.rosetta.java;

import com.asledgehammer.rosetta.NamedEntity;
import com.asledgehammer.rosetta.Notable;
import com.asledgehammer.rosetta.exception.MissingKeyException;
import com.asledgehammer.rosetta.exception.ValueTypeException;
import com.asledgehammer.rosetta.java.reference.ClassReference;
import com.asledgehammer.rosetta.java.reference.TypeReference;
import com.asledgehammer.rosetta.RosettaObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

public class JavaField extends RosettaObject
    implements JavaTyped, NamedEntity, Notable, Reflected<Field> {

  private final Field reflectedObject;
  private final String name;

  @Nullable private String notes;
  @Nullable private String deprecated;

  private boolean isNullable;
  private boolean isVolatile;
  private boolean isTransient;
  private boolean isStatic;
  private boolean isFinal;
  private boolean isNative;

  private JavaScope scope;

  private TypeReference type;

  private final List<String> tags = new ArrayList<>();

  JavaField(@NotNull Field field) {
    super();

    this.name = field.getName();
    this.reflectedObject = field;
    this.type = TypeReference.of(field.getGenericType());

    int modifiers = field.getModifiers();
    this.scope = JavaLanguage.getScope(modifiers);
    this.isNullable = !this.type.isPrimitive();
    this.isVolatile = JavaLanguage.isVolatile(modifiers);
    this.isTransient = JavaLanguage.isTransient(modifiers);
    this.isStatic = JavaLanguage.isStatic(modifiers);
    this.isFinal = JavaLanguage.isFinal(modifiers);
    this.isNative = JavaLanguage.isNative(modifiers);
  }

  JavaField(@NotNull String name, @NotNull Map<String, Object> raw) {
    super();

    this.name = name;
    this.reflectedObject = null;

    onLoad(raw);
  }

  protected void onLoad(@NotNull Map<String, Object> raw) {
    if (!raw.containsKey("type")) {
      throw new MissingKeyException(name, "type");
    }
    this.type = JavaLanguage.resolveType(raw.get("type"));

    // Process the scope enum.
    if (!raw.containsKey("scope")) {
      throw new MissingKeyException("fields[\"" + name + "\"]", "scope");
    }
    Object oScope = raw.get("scope");
    if (!(oScope instanceof String)) {
      throw new ValueTypeException(
          "fields[\"" + name + "\"]", "scope", oScope.getClass(), String.class);
    }

    this.scope = JavaScope.of((String) oScope);

    Object oFlag;

    // If defined, set the nullable flag.
    if (raw.containsKey("nullable")) {
      oFlag = raw.get("nullable");
      if (!(oFlag instanceof Boolean)) {
        throw new ValueTypeException("parameter", "nullable", oFlag.getClass(), Boolean.class);
      }
      this.isNullable = (boolean) (Boolean) oFlag;
    } else {
      this.isNullable = !type.isPrimitive();
    }

    // If defined, set the volatile flag.
    if (raw.containsKey("volatile")) {
      oFlag = raw.get("volatile");
      if (!(oFlag instanceof Boolean)) {
        throw new ValueTypeException("parameter", "volatile", oFlag.getClass(), Boolean.class);
      }
      this.isVolatile = (boolean) (Boolean) oFlag;
    } else {
      this.isVolatile = false;
    }

    // If defined, set the transient flag.
    if (raw.containsKey("transient")) {
      oFlag = raw.get("transient");
      if (!(oFlag instanceof Boolean)) {
        throw new ValueTypeException("parameter", "transient", oFlag.getClass(), Boolean.class);
      }
      this.isTransient = (boolean) (Boolean) oFlag;
    } else {
      this.isTransient = false;
    }

    // If defined, set the static flag.
    if (raw.containsKey("static")) {
      oFlag = raw.get("static");
      if (!(oFlag instanceof Boolean)) {
        throw new ValueTypeException("parameter", "static", oFlag.getClass(), Boolean.class);
      }
      this.isStatic = (boolean) (Boolean) oFlag;
    } else {
      this.isStatic = false;
    }

    // If defined, set the native flag.
    if (raw.containsKey("native")) {
      oFlag = raw.get("native");
      if (!(oFlag instanceof Boolean)) {
        throw new ValueTypeException("parameter", "native", oFlag.getClass(), Boolean.class);
      }
      this.isNative = (boolean) (Boolean) oFlag;
    } else {
      this.isNative = false;
    }

    // If defined, set the final flag.
    if (raw.containsKey("final")) {
      oFlag = raw.get("final");
      if (!(oFlag instanceof Boolean)) {
        throw new ValueTypeException("parameter", "final", oFlag.getClass(), Boolean.class);
      }
      this.isFinal = (boolean) (Boolean) oFlag;
    } else {
      this.isFinal = false;
    }
  }

  @NotNull
  protected Map<String, Object> onSave(
      @NotNull JavaSerializeInstance serialize, @NotNull ClassReference reference) {
    final Map<String, Object> raw = new HashMap<>();

    Class<?> deCl = reflectedObject.getDeclaringClass();
    if (serialize.hasTypeDictionary()) {
      raw.put("type", serialize.getTypeDictionary().register(serialize, type, reference, deCl));
    } else {
      raw.put("type", JavaLanguage.serializeType(serialize, type, reference, deCl));
    }
    raw.put("scope", this.scope.getID());

    if (isNullable()) raw.put("nullable", true);
    if (isVolatile()) raw.put("volatile", true);
    if (isTransient()) raw.put("transient", true);
    if (isNative()) raw.put("native", true);
    if (isFinal()) raw.put("final", true);
    if (isStatic()) raw.put("static", true);

    return raw;
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

  @Nullable
  @Override
  public Field getReflectionTarget() {
    return this.reflectedObject;
  }

  @NotNull
  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public boolean hasNotes() {
    return this.notes != null && !this.notes.isEmpty();
  }

  @Override
  @NotNull
  public String getNotes() {
    if (notes == null || this.notes.isEmpty()) {
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

  /**
   * @return True if the field is flagged as deprecated.
   */
  public boolean isDeprecated() {
    return this.deprecated != null;
  }

  /**
   * @return The deprecation message. If empty, the deprecation flag is set and no message is
   *     provided.
   * @throws NullPointerException If no deprecated message is set. (Use {@link
   *     JavaField#isDeprecated()} to check before invoking this method)
   */
  @NotNull
  public String getDeprecatedMessage() {
    if (this.deprecated == null) {
      throw new NullPointerException("The field is not deprecated. (No message set)");
    }
    return this.deprecated;
  }

  /**
   * Sets the deprecation flag of the field without a message.
   *
   * @param flag The flag to set.
   */
  public void setDeprecated(boolean flag) {
    String deprecated = flag ? "" : null;
    if (Objects.equals(this.deprecated, deprecated)) {
      return;
    }
    this.deprecated = deprecated;
    setDirty();
  }

  /**
   * @param message The message to set. If empty, the deprecation flag is set to true, but no
   *     message is provided. If null, the deprecation flag is set to false.
   */
  public void setDeprecated(@Nullable String message) {
    if (Objects.equals(this.deprecated, message)) {
      return;
    }
    this.deprecated = message;
    this.setDirty();
  }

  @Override
  public String toString() {
    return "JavaField \"" + getName() + "\"";
  }

  /**
   * @return True if one or more tags are applied.
   */
  public boolean hasTags() {
    return !this.tags.isEmpty();
  }

  /**
   * @return A read-only collection of applied tags.
   */
  @NotNull
  public List<String> getTags() {
    return Collections.unmodifiableList(tags);
  }

  /**
   * @param tag The tag to evaluate.
   * @return True if the tag is registered.
   * @throws NullPointerException If the tag is null.
   * @throws IllegalArgumentException If the tag is empty.
   */
  public boolean hasTag(@NotNull String tag) {
    if (tag.isEmpty()) {
      throw new IllegalArgumentException("The tag is empty.");
    }
    return this.tags.contains(tag);
  }

  /**
   * Applies a tag to the object.
   *
   * @param tag The tag to apply.
   * @throws NullPointerException If the tag is null.
   * @throws IllegalArgumentException If the tag is empty or already applied.
   */
  public void addTag(@NotNull String tag) {
    if (tag.isEmpty()) {
      throw new IllegalArgumentException("The tag is empty.");
    }
    if (tags.contains(tag)) {
      throw new IllegalArgumentException("The tag is already applied: " + tag);
    }
    this.tags.add(tag);
    setDirty();
  }

  /**
   * Removes a tag from the object.
   *
   * @param tag The tag to remove.
   * @throws NullPointerException If the tag is null.
   * @throws IllegalArgumentException If the tag is empty or is not applied.
   */
  public void removeTag(@NotNull String tag) {
    if (tag.isEmpty()) {
      throw new IllegalArgumentException("The tag is empty.");
    }
    if (!tags.contains(tag)) {
      throw new IllegalArgumentException("The tag is not applied: " + tag);
    }
    tags.remove(tag);
    setDirty();
  }

  /**
   * Clears all applied tags.
   *
   * @throws RuntimeException If the object has no tags. (Use {@link JavaField#hasTags()})
   */
  public void clearTags() {
    if (tags.isEmpty()) {
      throw new RuntimeException("No tags are registered.");
    }
    tags.clear();
  }

  public boolean isFinal() {
    return isFinal;
  }

  public void setFinal(boolean flag) {
    if (flag == isFinal) return;
    isFinal = flag;
    setDirty();
  }

  public boolean isNative() {
    return isNative;
  }

  public void setNative(boolean flag) {
    if (flag == isNative) return;
    isNative = flag;
    setDirty();
  }

  public boolean isStatic() {
    return isStatic;
  }

  public void setStatic(boolean flag) {
    if (flag == isStatic) return;
    isStatic = flag;
    setDirty();
  }

  public boolean isTransient() {
    return isTransient;
  }

  public void setTransient(boolean flag) {
    if (flag == isTransient) return;
    isTransient = flag;
    setDirty();
  }

  public boolean isVolatile() {
    return isVolatile;
  }

  public void setVolatile(boolean flag) {
    if (flag == isVolatile) return;
    isVolatile = flag;
    setDirty();
  }

  public boolean isNullable() {
    return isNullable;
  }

  public void setNullable(boolean flag) {
    if (flag == isNullable) return;
    this.isNullable = flag;
    setDirty();
  }
}
