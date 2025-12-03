package com.asledgehammer.rosetta.java;

import com.asledgehammer.rosetta.NamedEntity;
import com.asledgehammer.rosetta.Notable;
import com.asledgehammer.rosetta.RosettaObject;
import com.asledgehammer.rosetta.Taggable;
import com.asledgehammer.rosetta.exception.ClassAlreadyDiscoveredException;
import com.asledgehammer.rosetta.java.reference.ClassReference;
import com.asledgehammer.rosetta.java.reference.TypeReference;
import java.lang.reflect.*;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaClass extends RosettaObject
    implements NamedEntity, Notable, Reflected<Class<?>>, Taggable {

  private final Map<String, JavaClass> classes = new HashMap<>();
  private final Map<String, JavaField> fields = new HashMap<>();
  private final Map<String, JavaExecutableCollection<JavaMethod>> methods = new HashMap<>();
  private final List<JavaTypeParameter> typeParameters = new ArrayList<>();
  private List<String> tags = new ArrayList<>();
  private final JavaExecutableCollection<JavaConstructor> constructors;
  private ClassReference targetReference;
  private Class<?> target;
  private final JavaPackage pkg;
  private String notes;
  private final String name;

  private String deprecated;

  @Nullable private TypeReference extendz;
  @Nullable private List<TypeReference> implementz;

  private JavaScope scope;
  private boolean isStatic;
  private boolean isFinal;
  private boolean isAbstract;

  private boolean isDiscovered = false;

  JavaClass(@NotNull JavaPackage pkg, @NotNull Class<?> clazz) {
    super();

    this.pkg = pkg;
    this.name = clazz.getSimpleName();
    this.constructors = new JavaExecutableCollection<>(this.name);
  }

  JavaClass(
      @NotNull JavaPackage pkg,
      @NotNull JavaDeserializeInstance deserialize,
      @NotNull String name,
      @NotNull Map<String, Object> raw) {
    super();

    this.pkg = pkg;
    this.name = name;
    this.constructors = new JavaExecutableCollection<>(this.name);

    // Attempt to resolve reflection before loading.
    this.target = resolve(pkg.getPath() + "." + name);
    if (this.target != null) {
      this.targetReference = ClassReference.of(this.target);
    } else {
      this.targetReference = null;
    }

    onLoad(deserialize, raw);
  }

  void discover(
      @NotNull JavaDiscoverySettings settings,
      @NotNull Class<?> clazz,
      @NotNull JavaLanguage language) {

    if (isDiscovered()) {
      throw new ClassAlreadyDiscoveredException(this);
    }

    // Sanity-check.
    Class<?> target = getReflectionTarget();
    if (target != null && target != clazz) {
      throw new IllegalArgumentException(
          "The provided class doesn't match the reflection-target: (target: "
              + target.getName()
              + ", given: "
              + clazz.getName()
              + ")");
    }

    this.target = clazz;
    this.targetReference = ClassReference.of(clazz);

    final int modifiers = clazz.getModifiers();

    // Figure out the modifiers for the class.
    this.scope = JavaLanguage.getScope(modifiers);
    this.isStatic = JavaLanguage.isStatic(modifiers);
    this.isFinal = JavaLanguage.isFinal(modifiers);

    // Grab the superclass type.
    final Type clazzSuperGeneric = clazz.getGenericSuperclass();
    if (clazzSuperGeneric != null) {
      this.extendz = TypeReference.of(clazz.getGenericSuperclass());
    }

    // Grab any superinterface types.
    final Type[] clazzSuperInterfaces = clazz.getGenericInterfaces();
    if (clazzSuperInterfaces.length != 0) {
      implementz = new ArrayList<>();
      for (Type implement : clazz.getGenericInterfaces()) {
        implementz.add(TypeReference.of(implement));
      }
    }

    // Discover fields.
    if (settings.exposeFields()) {
      discoverFields(clazz);
    }

    // Discover constructors.
    if (settings.exposeConstructors()) {
      discoverConstructor(clazz);
    }

    // Discover methods.
    if (settings.exposeMethods()) {
      discoverMethods(clazz);
    }

    // If the exposure-policy of the settings passed are to expose any related classes.
    if (settings.getSuperPolicy() == JavaDiscoverySettings.SuperPolicy.EXPOSE) {
      // Ignore Object.class super-class being self-referencing.
      Class<?> clazzSuper = clazz.getSuperclass();
      if (clazzSuper != null && clazz.getSuperclass() != clazz) {
        language.of(settings, clazzSuper);
      }
      for (Class<?> interfaze : clazz.getInterfaces()) {
        language.of(settings, interfaze);
      }
    }

    // Lock discovery to prevent post-discovery mutations.
    this.isDiscovered = true;
  }

  private void discoverFields(@NotNull Class<?> clazz) {
    for (Field field : clazz.getDeclaredFields()) {
      fields.put(field.getName(), new JavaField(field));
    }
  }

  private void discoverConstructor(@NotNull Class<?> clazz) {
    for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
      JavaConstructor javaConstructor = new JavaConstructor(constructor);
      constructors.addExecutable(javaConstructor);
    }
  }

  private void discoverMethods(@NotNull Class<?> clazz) {
    for (Method method : clazz.getDeclaredMethods()) {
      String name = method.getName();
      JavaMethod javaMethod = new JavaMethod(method);
      JavaExecutableCollection<JavaMethod> collection =
          methods.computeIfAbsent(name, JavaExecutableCollection::new);
      collection.addExecutable(javaMethod);
    }
  }

  protected void onLoad(
      @NotNull JavaDeserializeInstance deserialize, @NotNull final Map<String, Object> raw) {

    final String label = "class[\"" + name + "\"]";

    // Load notes. (If defined)
    this.notes = getOptionalValue(raw, label, "notes", String.class);

    // Load tags. (If defined)
    this.tags = getOptionalStringList(raw, label, "tags");

    // Load the scope. (If defined. DEFAULT: "package")
    final String sScope = getOptionalValue(raw, label, "scope", "package", String.class);
    this.scope = JavaScope.of(sScope);

    // Boolean modifiers. (If defined)
    this.isAbstract = getOptionalValue(raw, label, "abstract", false, boolean.class);
    this.isStatic = getOptionalValue(raw, label, "static", false, boolean.class);
    this.isFinal = getOptionalValue(raw, label, "final", false, boolean.class);

    // Load the super-class type. (If defined)
    String oExtends = getOptionalValue(raw, label, "extends", String.class);
    // TODO: Implement Deserialize instance.
    this.extendz = oExtends != null ? JavaLanguage.resolveType(oExtends) : null;

    // Load any super-interface types. (If defined)
    List<?> list = getOptionalList(raw, label, "implements", Map.class, String.class);
    if (list != null) {
      for (Object oImplement : list) {
        // TODO: Implement Deserialize instance.
        implementz.add(JavaLanguage.resolveType(oImplement));
      }
    }

    // Load deprecated. (If defined)
    Object oDeprecated = getOptionalValue(raw, label, "deprecated", String.class, boolean.class);
    if (oDeprecated != null) {
      if (oDeprecated instanceof String) {
        this.deprecated = (String) oDeprecated;
      } else if (oDeprecated instanceof Boolean) {
        this.deprecated = (boolean) oDeprecated ? "" : null;
      }
    }

    // Load any type_parameters. (If defined)
    final List<Map<String, Object>> listTypeParameters =
        getOptionalDictionaryList(raw, label, "type_parameters");
    if (listTypeParameters != null && !listTypeParameters.isEmpty()) {
      for (final Map<String, Object> oTypeParameter : listTypeParameters) {
        // TODO: Implement Deserialize instance.
        final JavaTypeParameter javaTypeParameter =
            new JavaTypeParameter(JavaLanguage.resolveType(oTypeParameter));
        this.typeParameters.add(javaTypeParameter);
      }
    }

    // Load classes. (If any)
    final Map<String, Object> classes = getOptionalDictionary(raw, label, "classes");
    if (classes != null && !classes.isEmpty()) {
      for (final String key : classes.keySet()) {
        final String labelClass = label + "classes[\"" + key + "\"]";
        final Map<String, Object> classRaw = getExpectedDictionary(classes, labelClass, key);
        this.classes.put(key, new JavaClass(this.pkg, deserialize, key, classRaw));
      }
    }

    // Load any fields. (If defined)
    final Map<String, Object> oFields = getOptionalDictionary(raw, label, "fields");
    if (oFields != null && !oFields.isEmpty()) {
      for (String key : oFields.keySet()) {
        final String labelField = label + ".fields[\"" + key + "\"]";
        oFields.put(key, new JavaField(key, getExpectedDictionary(oFields, labelField, key)));
      }
    }

    // Load any constructors. (If defined)
    final List<Map<String, Object>> oConstructors =
        getOptionalDictionaryList(raw, label, "constructors");
    if (oConstructors != null && !oConstructors.isEmpty()) {
      for (Map<String, Object> oConstructor : oConstructors) {
        constructors.addExecutable(new JavaConstructor(this.name, oConstructor));
      }
    }

    // Load any methods. (If defined)
    final List<Map<String, Object>> oMethods = getOptionalDictionaryList(raw, label, "methods");
    if (oMethods != null) {
      for (int i = 0; i < oMethods.size(); i++) {
        final Map<String, Object> oMethod = oMethods.get(i);
        final String labelMethod = label + ".methods[\"" + i + "\"]";
        final String name = getExpectedValue(oMethod, labelMethod, "name", String.class);
        final JavaExecutableCollection<JavaMethod> methods =
            this.methods.computeIfAbsent(name, JavaExecutableCollection::new);
        methods.addExecutable(new JavaMethod(name, raw));
      }
    }
  }

  @NotNull
  protected Map<String, Object> onSave(@NotNull JavaSerializeInstance serialize) {

    final Map<String, Object> raw = new HashMap<>();

    if (scope != JavaScope.PACKAGE) {
      raw.put("scope", scope.getID());
    }

    if (isStatic) {
      raw.put("static", true);
    }

    if (isFinal) {
      raw.put("final", true);
    }

    if (!typeParameters.isEmpty()) {
      final List<Object> listTypeParameters = new ArrayList<>();
      for (JavaTypeParameter typeParameter : typeParameters) {
        listTypeParameters.add(typeParameter.onSave(serialize, targetReference, target));
      }
      raw.put("type_parameters", listTypeParameters);
    }

    // Serialize the super-class type.
    if (this.extendz != null) {
      if (serialize.hasTypeDictionary()) {
        raw.put(
            "extends",
            serialize
                .getTypeDictionary()
                .register(serialize, this.extendz, targetReference, target));
      } else {
        raw.put(
            "extends",
            JavaLanguage.serializeType(serialize, this.extendz, targetReference, target));
      }
    }

    // Serialize any super-interface type(s).
    if (this.implementz != null && !this.implementz.isEmpty()) {
      // TODO: Implement with Serialize Type-Dictionary.
      final List<Object> implementz = new ArrayList<>();
      if (serialize.hasTypeDictionary()) {
        final JavaTypeDictionary typeDictionary = serialize.getTypeDictionary();
        for (TypeReference implement : this.implementz) {
          implementz.add(typeDictionary.register(serialize, implement, targetReference, target));
        }
      } else {
        for (TypeReference implement : this.implementz) {
          implementz.add(JavaLanguage.serializeType(serialize, implement, targetReference, target));
        }
      }
      raw.put("implements", implementz);
    }

    if (deprecated != null) {
      if (deprecated.isEmpty()) {
        // Non-descriptive.
        raw.put("deprecated", true);
      } else {
        // Descriptive.
        raw.put("deprecated", deprecated);
      }
    }

    // If the class has documentation notes, save them.
    if (notes != null && !notes.isEmpty()) {
      raw.put("notes", notes);
    }

    // If tags are assigned to the class, save them.
    if (hasTags()) {
      raw.put("tags", getTags());
    }

    // If the class has classes, save them.
    if (hasClasses()) {
      final Map<String, Object> classes = new HashMap<>();
      final List<String> keys = new ArrayList<>(this.classes.keySet());
      keys.sort(Comparator.naturalOrder());
      for (String key : keys) {
        JavaClass javaClass = this.classes.get(key);
        classes.put(key, javaClass.onSave(serialize));
      }
      raw.put("classes", classes);
    }

    // If the class has fields, save them.
    if (hasFields()) {
      final Map<String, Object> fields = new HashMap<>();
      final List<String> keys = new ArrayList<>(this.fields.keySet());
      keys.sort(Comparator.naturalOrder());
      for (String key : keys) {
        JavaField javaField = this.fields.get(key);
        fields.put(key, javaField.onSave(serialize, this.targetReference));
      }
      raw.put("fields", fields);
    }

    // If the class has constructors, save them.
    if (hasConstructors()) {
      final List<Map<String, Object>> constructors = new ArrayList<>();
      final List<JavaConstructor> javaConstructors =
          new ArrayList<>(this.constructors.getExecutables());
      javaConstructors.sort(Comparator.comparing(JavaExecutable::getSignature));
      for (JavaConstructor constructor : javaConstructors) {
        constructors.add(constructor.onSave(serialize, targetReference));
      }
      raw.put("constructors", constructors);
    }

    if (hasMethods()) {
      final List<Map<String, Object>> methods = new ArrayList<>();

      // Go through each method alphanumerically.
      List<String> keys = new ArrayList<>(this.methods.keySet());
      keys.sort(Comparator.naturalOrder());

      for (String key : keys) {

        // Grab each group and sort them by signatures for clean exports.
        final JavaExecutableCollection<JavaMethod> methodGroup = this.methods.get(key);
        final List<JavaMethod> javaMethods = new ArrayList<>(methodGroup.getExecutables());
        if (javaMethods.size() > 1) {
          javaMethods.sort(Comparator.comparing(JavaExecutable::getSignature));
        }
        for (JavaMethod method : javaMethods) {
          methods.add(method.onSave(serialize, targetReference));
        }
      }
      raw.put("methods", methods);
    }

    return raw;
  }

  @Override
  public String toString() {
    return "JavaClass \"" + getPackage().getPath() + "." + getName() + "\"";
  }

  @NotNull
  @Override
  public String getName() {
    return name;
  }

  public boolean hasClasses() {
    return !this.classes.isEmpty();
  }

  @NotNull
  public Map<String, JavaClass> getClasses() {
    return this.classes;
  }

  public void addClass(@NotNull JavaClass javaClass) {
    String key = javaClass.getName();
    if (this.classes.containsKey(key)) {
      throw new IllegalArgumentException(
          "A class definition is already registered for the name: " + key);
    }
    this.classes.put(javaClass.getName(), javaClass);
  }

  public void removeClass(@NotNull JavaClass javaClass) {
    String key = javaClass.getName();
    if (!this.classes.containsKey(key)) {
      throw new IllegalArgumentException(
          "A class definition is NOT registered with the name: " + key);
    }
    this.classes.remove(javaClass.getName());
  }

  @NotNull
  public JavaClass removeClass(@NotNull String clazzName) {
    if (!this.classes.containsKey(clazzName)) {
      throw new IllegalArgumentException(
          "A class definition is NOT registered with the name: " + clazzName);
    }
    return this.classes.remove(clazzName);
  }

  @NotNull
  public List<JavaTypeParameter> getTypeParameters() {
    return this.typeParameters;
  }

  @NotNull
  public JavaExecutableCollection<JavaConstructor> getConstructors() {
    return this.constructors;
  }

  public boolean hasConstructors() {
    return !this.constructors.isEmpty();
  }

  @NotNull
  public Map<String, JavaExecutableCollection<JavaMethod>> getMethods() {
    return this.methods;
  }

  public boolean hasMethods() {
    return !this.methods.isEmpty();
  }

  @NotNull
  public Map<String, JavaField> getFields() {
    return this.fields;
  }

  public boolean hasFields() {
    return !this.fields.isEmpty();
  }

  @NotNull
  public JavaMethod getMethod(@NotNull Method method) {
    String name = method.getName();
    JavaExecutableCollection<JavaMethod> methods = this.methods.get(name);
    return methods.getExecutable(method);
  }

  @Nullable
  @Override
  public Class<?> getReflectionTarget() {
    return this.target;
  }

  void setReflectedObject(@Nullable Class<?> target) {
    this.target = target;
  }

  @NotNull
  public JavaPackage getPackage() {
    return pkg;
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
    if (!hasTags()) {
      throw new RuntimeException("No tags are registered.");
    }
    List<String> tagsRemoved = Collections.unmodifiableList(tags);
    tags.clear();
    return tagsRemoved;
  }

  boolean isDiscovered() {
    return isDiscovered;
  }

  @Nullable
  public static Class<?> resolve(@NotNull String path) {
    return resolve(path, ClassLoader.getSystemClassLoader());
  }

  @Nullable
  public static Class<?> resolve(@NotNull String path, @NotNull ClassLoader classLoader) {
    try {
      return Class.forName(path, false, classLoader);
    } catch (Exception e) {
      return null;
    }
  }
}
