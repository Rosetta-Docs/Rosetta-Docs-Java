package com.asledgehammer.rosetta.java;

import com.asledgehammer.rosetta.NamedEntity;
import com.asledgehammer.rosetta.Notable;
import com.asledgehammer.rosetta.RosettaObject;
import com.asledgehammer.rosetta.Taggable;
import com.asledgehammer.rosetta.exception.ClassAlreadyDiscoveredException;
import com.asledgehammer.rosetta.exception.MissingKeyException;
import com.asledgehammer.rosetta.exception.ValueTypeException;
import com.asledgehammer.rosetta.java.reference.ClassReference;
import com.asledgehammer.rosetta.java.reference.TypeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;

public class JavaClass extends RosettaObject
    implements NamedEntity, Notable, Reflected<Class<?>>, Taggable {

  private final Map<String, JavaClass> classes = new HashMap<>();
  private final Map<String, JavaField> fields = new HashMap<>();
  private final Map<String, JavaExecutableCollection<JavaMethod>> methods = new HashMap<>();
  private final List<JavaTypeParameter> typeParameters = new ArrayList<>();
  private final List<String> tags = new ArrayList<>();
  private final JavaExecutableCollection<JavaConstructor> constructors;
  private ClassReference targetReference;
  private Class<?> target;
  private final JavaPackage pkg;
  private String notes;
  private final String name;

  private String deprecated;

  @Nullable private TypeReference extendz;
  private List<TypeReference> implementz = new ArrayList<>();

  private JavaScope scope;
  private boolean isStatic;
  private boolean isFinal;

  private boolean isDiscovered = false;

  JavaClass(@NotNull JavaPackage pkg, @NotNull Class<?> clazz) {
    super();

    this.pkg = pkg;
    this.name = clazz.getSimpleName();
    this.constructors = new JavaExecutableCollection<>(this.name);
  }

  JavaClass(@NotNull JavaPackage pkg, @NotNull String name, @NotNull Map<String, Object> raw) {
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

    onLoad(raw);
  }

  void discover(
      @NotNull Class<?> clazz, @NotNull JavaLanguage language, @NotNull ExposureSettings settings) {

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
    for (Type implement : clazz.getGenericInterfaces()) {
      implementz.add(TypeReference.of(implement));
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
    if (settings.getSuperPolicy() == ExposureSettings.SuperPolicy.EXPOSE) {
      // Ignore Object.class super-class being self-referencing.
      Class<?> clazzSuper = clazz.getSuperclass();
      if (clazzSuper != null && clazz.getSuperclass() != clazz) {
        language.of(clazzSuper, settings);
      }
      for (Class<?> interfaze : clazz.getInterfaces()) {
        language.of(interfaze, settings);
      }
    }

    // Lock discovery to prevent post-discovery mutations.
    this.isDiscovered = true;
  }

  private void discoverFields(@NotNull Class<?> clazz) {
    for (Field field : clazz.getDeclaredFields()) {
      JavaField javaField = new JavaField(field);
      fields.put(javaField.getName(), javaField);
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

  @Override
  public boolean onCompile() {

    // Compile field(s).
    if (!fields.isEmpty()) {
      for (JavaField field : fields.values()) {
        if (field.isDirty()) {
          // Fail compilation if field fails to compile.
          if (!field.compile()) {
            return false;
          }
        }
      }
    }

    // Compile method(s).
    if (!methods.isEmpty()) {
      for (JavaExecutableCollection<JavaMethod> methodList : methods.values()) {
        for (JavaMethod method : methodList.getExecutables()) {
          if (method.isDirty()) {
            // Fail compilation if method fails to compile.
            if (!method.compile()) {
              return false;
            }
          }
        }
      }
    }

    // Compile constructor(s).
    if (!constructors.isEmpty()) {
      for (JavaConstructor constructor : constructors.getExecutables()) {
        if (constructor.isDirty()) {
          // Fail compilation if constructor fails to compile.
          if (!constructor.compile()) {
            return false;
          }
        }
      }
    }

    return true;
  }

  protected void onLoad(@NotNull final Map<String, Object> raw) {

    // If the class is defined as static.
    if (raw.containsKey("static")) {
      Object oStatic = raw.get("static");
      if (!(oStatic instanceof Boolean)) {
        throw new ValueTypeException("class", "static", oStatic.getClass(), Boolean.class);
      }
      this.isStatic = (boolean) (Boolean) oStatic;
    } else {
      this.isStatic = false;
    }

    // If the class is defined as final.
    if (raw.containsKey("final")) {
      Object oFinal = raw.get("final");
      if (!(oFinal instanceof Boolean)) {
        throw new ValueTypeException("class", "final", oFinal.getClass(), Boolean.class);
      }
      this.isFinal = (boolean) (Boolean) oFinal;
    } else {
      this.isFinal = false;
    }

    // If the class extends another, resolve the type.
    if (raw.containsKey("extends")) {
      this.extendz = JavaLanguage.resolveType(raw.get("extends"));
    }

    // Any implementation types are resolved.
    if (raw.containsKey("implements")) {
      Object oImplements = raw.get("implements");
      if (!(oImplements instanceof List)) {
        throw new ValueTypeException("class", "implements", oImplements.getClass(), List.class);
      }
      for (Object oImplement : (List<Object>) oImplements) {
        implementz.add(JavaLanguage.resolveType(oImplement));
      }
    }

    // If the object is deprecated and optionally has a description for it.
    if (raw.containsKey("deprecated")) {
      Object oDeprecated = raw.get("deprecated");
      String deprecated;
      if (oDeprecated instanceof String) {
        deprecated = (String) oDeprecated;
      } else if (oDeprecated instanceof Boolean) {
        deprecated = (boolean) (Boolean) oDeprecated ? "" : null;
      } else {
        throw new ValueTypeException(
            "class", "deprecated", oDeprecated.getClass(), String.class, Boolean.class);
      }
      this.deprecated = deprecated;
    }

    // Load notes. (If defined)
    if (raw.containsKey("notes")) {
      Object oNotes = raw.get("notes");
      if (!(oNotes instanceof String)) {
        throw new ValueTypeException("class", "notes", oNotes.getClass(), String.class);
      }
    }

    // Load any type_parameters. (If defined)
    if (raw.containsKey("type_parameters")) {
      Object oTypeParameters = raw.get("type_parameters");
      if (!(oTypeParameters instanceof List)) {
        throw new ValueTypeException(
            "class", "type_parameters", oTypeParameters.getClass(), List.class);
      }

      for (Object oTypeParameter : (List<Object>) oTypeParameters) {
        JavaTypeParameter javaTypeParameter =
            new JavaTypeParameter(JavaLanguage.resolveType(oTypeParameter));
        this.typeParameters.add(javaTypeParameter);
      }
    }

    // Load any sub-classes. (If defined)
    if (raw.containsKey("classes")) {
      Object oClasses = raw.get("classes");
      if (!(oClasses instanceof Map)) {
        throw new ValueTypeException("class", "classes", oClasses.getClass(), Map.class);
      }
      Map<String, Object> classes = (Map<String, Object>) oClasses;
      for (String key : classes.keySet()) {
        Object oClass = classes.get(key);
        if (!(oClass instanceof Map)) {
          throw new ValueTypeException("class.classes", key, oClass.getClass(), Map.class);
        }
        this.classes.put(key, new JavaClass(this.pkg, key, (Map<String, Object>) oClass));
      }
    }

    // Load any fields. (If defined)
    if (raw.containsKey("fields")) {
      Object oFields = raw.get("fields");
      if (!(oFields instanceof Map)) {
        throw new ValueTypeException("class", "fields", oFields.getClass(), Map.class);
      }
      Map<String, Object> fields = (Map<String, Object>) oFields;
      //      List<String> keys = new ArrayList<>(fields.keySet());
      //      keys.sort(Comparator.naturalOrder());

      for (String key : fields.keySet()) {
        Object oField = fields.get(key);
        if (!(oField instanceof Map)) {
          throw new ValueTypeException("class.fields", key, oField.getClass(), Map.class);
        }
        fields.put(key, new JavaField(key, (Map<String, Object>) oField));
      }
    }

    // Load any constructors. (If defined)
    if (raw.containsKey("constructors")) {
      Object oConstructors = raw.get("constructors");
      if (!(oConstructors instanceof List)) {
        throw new ValueTypeException("class", "constructors", oConstructors.getClass(), List.class);
      }
      List<Object> objects = (List<Object>) oConstructors;
      for (int i = 0; i < objects.size(); i++) {
        Object oConstructor = objects.get(i);
        if (!(oConstructor instanceof Map)) {
          throw new ValueTypeException(
              "class", "constructors[" + i + "]", oConstructor.getClass(), Map.class);
        }
        constructors.addExecutable(
            new JavaConstructor(this.name, (Map<String, Object>) oConstructor));
      }
    }

    // Load any methods. (If defined)
    if (raw.containsKey("methods")) {
      Object oMethods = raw.get("methods");
      if (!(oMethods instanceof List)) {
        throw new ValueTypeException("class", "methods", oMethods.getClass(), List.class);
      }
      List<Object> objects = (List<Object>) oMethods;
      for (int i = 0; i < objects.size(); i++) {
        Object oMethod = objects.get(i);
        if (!(oMethod instanceof Map)) {
          throw new ValueTypeException(
              "class", "methods[" + i + "]", oMethod.getClass(), Map.class);
        }
        Map<String, Object> method = (Map<String, Object>) oMethod;
        if (!method.containsKey("name")) {
          throw new MissingKeyException("class.methods[" + i + "]", "name");
        }
        Object oName = method.get("name");
        if (!(oName instanceof String methodName)) {
          throw new ValueTypeException(
              "class.methods[" + i + "]", "name", oName.getClass(), String.class);
        }
        JavaExecutableCollection<JavaMethod> methods =
            this.methods.computeIfAbsent(methodName, JavaExecutableCollection::new);
        methods.addExecutable(new JavaMethod(methodName, raw));
      }
    }
  }

  @NotNull
  protected Map<String, Object> onSave() {

    final Map<String, Object> raw = new HashMap<>();

    if (isStatic) {
      raw.put("static", true);
    }

    if (isFinal) {
      raw.put("final", true);
    }

    if (!typeParameters.isEmpty()) {
      final List<Object> listTypeParameters = new ArrayList<>();
      for (JavaTypeParameter typeParameter : typeParameters) {
        listTypeParameters.add(typeParameter.onSave(targetReference, target));
      }
      raw.put("type_parameters", listTypeParameters);
    }

    // Serialize the super-class type.
    if (this.extendz != null) {
      raw.put("extends", JavaLanguage.serializeType(this.extendz, targetReference, target));
    }

    // Serialize any super-interface type(s).
    if (this.implementz != null && !this.implementz.isEmpty()) {
      final List<Object> implementz = new ArrayList<>();
      for (TypeReference implement : this.implementz) {
        implementz.add(JavaLanguage.serializeType(implement, targetReference, target));
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
        classes.put(key, javaClass.onSave());
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
        fields.put(key, javaField.onSave(this.targetReference));
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
        constructors.add(constructor.onSave(targetReference));
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
          methods.add(method.onSave(targetReference));
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
    if (isDirty()) compile();
    return this.typeParameters;
  }

  @NotNull
  public JavaExecutableCollection<JavaConstructor> getConstructors() {
    if (isDirty()) compile();
    return this.constructors;
  }

  public boolean hasConstructors() {
    return !this.constructors.isEmpty();
  }

  @NotNull
  public Map<String, JavaExecutableCollection<JavaMethod>> getMethods() {
    if (isDirty()) compile();
    return this.methods;
  }

  public boolean hasMethods() {
    return !this.methods.isEmpty();
  }

  @NotNull
  public Map<String, JavaField> getFields() {
    if (isDirty()) compile();
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
    notes = notes == null || notes.isEmpty() ? null : notes;

    // Catch redundant changes to not set dirty flag.
    if (this.notes == null) {
      if (notes == null) return;
    } else if (this.notes.equals(notes)) return;

    this.notes = notes;

    setDirty();
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
    setDirty();
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
    setDirty();
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
