package com.asledgehammer.rosetta.java;

import com.asledgehammer.rosetta.DirtySupported;
import com.asledgehammer.rosetta.NamedEntity;
import com.asledgehammer.rosetta.Notable;
import com.asledgehammer.rosetta.RosettaObject;
import com.asledgehammer.rosetta.exception.ValueTypeException;
import com.asledgehammer.rosetta.java.reference.ClassReference;
import com.asledgehammer.rosetta.java.reference.TypeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @param <E> The type of {@link Executable} ({@link Method} or {@link Constructor})
 */
public abstract class JavaExecutable<E extends Executable> extends RosettaObject
    implements DirtySupported, NamedEntity, Notable, Reflected<E> {

  /** Used to validate method names passed from serialized files. */
  private static final Pattern REGEX_EXECUTABLE_NAME =
      Pattern.compile("^[a-zA-Z_$][a-zA-Z0-9_$]*$");

  /** Used to prevent wasteful empty list instantiations in heap memory. */
  private static final List<JavaParameter> DEFAULT_EMPTY_LIST = List.of();

  private final List<JavaParameter> parameters = new ArrayList<>();
  private final List<JavaTypeParameter> typeParameters = new ArrayList<>();

  private final String signature;
  protected final String name;
  private JavaScope scope;

  private String notes;

  /** Defaults to true to compile first-time. */
  private boolean dirty = true;

  /**
   * Compiled when dirty to keep properties clean and forcefully runs through getter/setters for the
   * record.
   */
  private List<JavaParameter> parametersReadOnly = DEFAULT_EMPTY_LIST;

  private final E target;

  @Nullable private String deprecated;

  /**
   * New Constructor for Method and Constructors.
   *
   * <p>NOTE: They must populate their properties in the sub-constructor calling this one.
   *
   * @param executable The executable reflection object.
   */
  protected JavaExecutable(@NotNull E executable) {
    super();

    this.target = executable;
    this.name = executable.getName();
    this.signature = createSignature(this);

    this.scope = JavaLanguage.getScope(executable);

    // Register any generic parameter variables.
    TypeVariable<?>[] typeVariables = executable.getTypeParameters();
    for (TypeVariable<?> typeVariable : typeVariables) {
      this.typeParameters.add(new JavaTypeParameter(TypeReference.of(typeVariable)));
    }

    // If parameters are provided, add them.
    if (executable.getParameterCount() != 0) {
      for (Parameter parameter : executable.getParameters()) {
        this.parameters.add(new JavaParameter(parameter));
      }
    }
  }

  protected JavaExecutable(@NotNull String name, @NotNull Map<String, Object> raw) {
    super();

    this.name = name;
    this.target = null;
    onLoad(raw);
    this.signature = createSignature(this);
  }

  @SuppressWarnings({"unchecked"})
  protected void onLoad(@NotNull Map<String, Object> raw) {
    // Load parameters. (If present)
    if (raw.containsKey("parameters")) {
      Object oParameters = raw.get("parameters");
      if (!(oParameters instanceof List)) {
        throw new ValueTypeException(name, "parameters", oParameters.getClass(), List.class);
      }
      List<Object> objects = (List<Object>) oParameters;
      for (int i = 0; i < objects.size(); i++) {
        Object oParameter = objects.get(i);
        if (!(oParameter instanceof Map)) {
          throw new ValueTypeException(
              name, "parameters[" + i + "]", oParameter.getClass(), Map.class);
        }
        parameters.add(new JavaParameter((Map<String, Object>) oParameter));
      }
    }

    // Load scope. (If defined; DEFAULT: "package")
    if (raw.containsKey("scope")) {
      Object oScope = raw.get("scope");
      if (!(oScope instanceof String)) {
        throw new ValueTypeException(name, "scope", oScope.getClass(), String.class);
      }
      this.scope = JavaScope.of((String) oScope);
    } else {
      this.scope = JavaScope.PACKAGE;
    }

    if (raw.containsKey("deprecated")) {
      Object oDeprecated = raw.get("deprecated");
      if (oDeprecated instanceof String) {
        this.deprecated = (String) oDeprecated;
      } else if (oDeprecated instanceof Boolean) {
        this.deprecated = (boolean) oDeprecated ? "" : null;
      }
    }

    // Load notes. (If present)
    if (raw.containsKey("notes")) {
      this.notes = raw.get("notes").toString();
    }
  }

  @NotNull
  protected Map<String, Object> onSave(
      @NotNull JavaSerializeInstance serialize, @NotNull ClassReference reference) {
    Map<String, Object> raw = new HashMap<>();

    final Class<?> deCl = getReflectionTarget().getDeclaringClass();

    if (scope != JavaScope.PACKAGE) {
      raw.put("scope", scope.getID());
    }

    if (hasNotes()) {
      raw.put("notes", getNotes());
    }

    if (isDeprecated()) {
      raw.put("deprecated", true);
    }

    if (hasTypeParameters()) {
      final List<Map<String, Object>> typeParameters = new ArrayList<>();
      for (JavaTypeParameter parameter : this.typeParameters) {
        typeParameters.add(parameter.onSave(serialize, reference, deCl));
      }
      raw.put("type_parameters", typeParameters);
    }

    if (hasParameters()) {
      final List<Map<String, Object>> parameters = new ArrayList<>();
      for (JavaParameter javaParameter : this.parameters) {
        parameters.add(javaParameter.onSave(serialize, reference, deCl));
      }
      raw.put("parameters", parameters);
    }

    return raw;
  }

  @Override
  public boolean onCompile() {

    // Compile read-only list.
    if (this.hasParameters()) {

      // If any parameter is dirty, also compile.
      for (JavaParameter parameter : parameters) {
        if (parameter.isDirty()) {

          // Fail compilation if the parameter fails.
          if (!parameter.compile()) {
            return false;
          }
        }
      }

      this.parametersReadOnly = Collections.unmodifiableList(this.parameters);
    } else {
      this.parametersReadOnly = DEFAULT_EMPTY_LIST;
    }

    return true;
  }

  @Override
  public boolean isDirty() {

    // Normal dirty-flag check.
    if (this.dirty) return true;

    // Check parameter(s) for dirty flag.
    for (JavaParameter parameter : getParameters()) {
      if (parameter.isDirty()) {
        return true;
      }
    }

    // Methods will need to implement this by a super-call AND check return definition.

    return false;
  }

  @Override
  public void setDirty(boolean flag) {
    this.dirty = flag;
  }

  /**
   * @return True if the executable has no parameter definitions.
   */
  public boolean hasParameters() {
    return !this.parameters.isEmpty();
  }

  public boolean hasTypeParameters() {
    return !this.typeParameters.isEmpty();
  }

  /**
   * @return A read-only list of registered parameters for the executable definition.
   */
  @NotNull
  public List<JavaParameter> getParameters() {

    // Check to see if any parameter(s) were modified or added / removed to the executable. If so,
    // recompile to rebuild the list we return.
    if (isDirty()) {
      compile();
    }

    // Return the read-only list to prevent unintentional data-poisoning.
    return this.parametersReadOnly;
  }

  /**
   * @return The string-serialized signature.
   */
  @NotNull
  public String getSignature() {
    return this.signature;
  }

  @NotNull
  @Override
  public String getName() {
    return this.name;
  }

  @NotNull
  @Override
  public E getReflectionTarget() {
    return this.target;
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

  /**
   * Serializes a java-executable definition as a signature string.
   *
   * @param executable The executable to serialize.
   * @return The serialized signature.
   * @throws NullPointerException If the executable is null.
   */
  public static String createSignature(@NotNull JavaExecutable<?> executable) {
    // TODO: Implement.
    return "";
  }

  /**
   * Tests if a Java executable name is valid.
   *
   * @param name The name to test.
   * @return True if the executable's name is valid.
   * @throws NullPointerException If the name is null.
   */
  public static boolean isValidName(@NotNull String name) {
    return !name.isEmpty() && REGEX_EXECUTABLE_NAME.matcher(name).find();
  }
}
