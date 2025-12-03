package com.asledgehammer.rosetta.java;

import com.asledgehammer.rosetta.NamedEntity;
import com.asledgehammer.rosetta.Notable;
import com.asledgehammer.rosetta.RosettaObject;
import com.asledgehammer.rosetta.Taggable;
import com.asledgehammer.rosetta.java.reference.ClassReference;
import com.asledgehammer.rosetta.java.reference.TypeReference;
import java.lang.reflect.Field;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaField extends RosettaObject
    implements JavaTyped, NamedEntity, Notable, Reflected<Field>, Taggable {

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

    final String label = "fields[\"" + name + "\"]";

    // Load the type.
    Object oType = getExpectedValue(raw, label, "type", Map.class, String.class);
    this.type = JavaLanguage.resolveType(oType);

    // Load the scope. (If defined. Default is 'package')
    final String sScope = getOptionalValue(raw, label, "scope", "package", String.class);
    this.scope = JavaScope.of(sScope);

    // Boolean modifiers. (If defined)
    this.isNullable = getOptionalValue(raw, label, "nullable", !type.isPrimitive(), boolean.class);
    this.isVolatile = getOptionalValue(raw, label, "volatile", false, boolean.class);
    this.isTransient = getOptionalValue(raw, label, "transient", false, boolean.class);
    this.isStatic = getOptionalValue(raw, label, "static", false, boolean.class);
    this.isNative = getOptionalValue(raw, label, "native", false, boolean.class);
    this.isFinal = getOptionalValue(raw, label, "final", false, boolean.class);
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

    if (scope != JavaScope.PACKAGE) {
      raw.put("scope", scope.getID());
    }

    if (!isNullable()) raw.put("nullable", false);
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
    this.notes = notes == null || notes.isEmpty() ? null : notes;
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
  }

  @Override
  public String toString() {
    return "JavaField \"" + getName() + "\"";
  }

  @Override
  public boolean hasTags() {
    return !this.tags.isEmpty();
  }

  @NotNull
  @Override
  public List<String> getTags() {
    return Collections.unmodifiableList(tags);
  }

  @Override
  public boolean hasTag(@NotNull String tag) {
    if (tag.isEmpty()) {
      throw new IllegalArgumentException("The tag is empty.");
    }
    return this.tags.contains(tag);
  }

  @Override
  public void addTag(@NotNull String tag) {
    if (tag.isEmpty()) {
      throw new IllegalArgumentException("The tag is empty.");
    }
    if (tags.contains(tag)) {
      throw new IllegalArgumentException("The tag is already applied: " + tag);
    }
    this.tags.add(tag);
  }

  @Override
  public void removeTag(@NotNull String tag) {
    if (tag.isEmpty()) {
      throw new IllegalArgumentException("The tag is empty.");
    }
    if (!tags.contains(tag)) {
      throw new IllegalArgumentException("The tag is not applied: " + tag);
    }
    tags.remove(tag);
  }

  @NotNull
  @Override
  public List<String> clearTags() {
    if (tags.isEmpty()) {
      throw new RuntimeException("No tags are registered.");
    }
    List<String> toReturn = new ArrayList<>(tags);
    tags.clear();
    return toReturn;
  }

  public boolean isFinal() {
    return isFinal;
  }

  public void setFinal(boolean flag) {
    if (flag == isFinal) return;
    isFinal = flag;
  }

  public boolean isNative() {
    return isNative;
  }

  public void setNative(boolean flag) {
    if (flag == isNative) return;
    isNative = flag;
  }

  public boolean isStatic() {
    return isStatic;
  }

  public void setStatic(boolean flag) {
    if (flag == isStatic) return;
    isStatic = flag;
  }

  public boolean isTransient() {
    return isTransient;
  }

  public void setTransient(boolean flag) {
    if (flag == isTransient) return;
    isTransient = flag;
  }

  public boolean isVolatile() {
    return isVolatile;
  }

  public void setVolatile(boolean flag) {
    if (flag == isVolatile) return;
    isVolatile = flag;
  }

  public boolean isNullable() {
    return isNullable;
  }

  public void setNullable(boolean flag) {
    if (flag == isNullable) return;
    this.isNullable = flag;
  }
}
