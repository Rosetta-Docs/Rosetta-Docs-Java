package com.asledgehammer.rosetta.java;

import com.asledgehammer.rosetta.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

public class JavaPackage extends RosettaObject
    implements DirtySupported, NamedEntity, Notable, Reflected<Package>, Taggable {

  /** To test Java package names for validity. */
  private static final Pattern REGEX_PKG_NAME =
      Pattern.compile("^[a-z_][a-z0-9_]*$", Pattern.CASE_INSENSITIVE);

  /** Stores class definitions in the package. */
  private final Map<String, JavaClass> classes = new HashMap<>();

  /** Stores sub-package definitions. */
  private final Map<String, JavaPackage> packages = new HashMap<>();

  private Package target;

  /** The package name. */
  private final String name;

  private final String path;

  private final JavaPackage parent;

  private final JavaLanguage language;

  /** Package-Info documentation notes. */
  private String notes;

  private final List<String> tags = new ArrayList<>();

  /**
   * Creation constructor for new package definitions.
   *
   * @param pkg The Java-Reflection package instance.
   */
  JavaPackage(@NotNull JavaLanguage lang, @Nullable JavaPackage parent, @NotNull Package pkg) {
    super();

    this.language = lang;
    this.parent = parent;
    this.target = pkg;

    // We already know that this is a valid package-name.
    this.path = pkg.getName();
    if (this.path.contains(".")) {
      String[] split = path.split("\\.");
      this.name = split[split.length - 1];
    } else {
      this.name = this.path;
    }
    if (this.parent != null) {
      this.parent.addPackage(this);
    }
  }

  /**
   * Creation constructor used for packages without any classes in them. (Package object passed is
   * null)
   *
   * @param lang
   * @param parent
   * @param name
   */
  JavaPackage(@NotNull JavaLanguage lang, @Nullable JavaPackage parent, @NotNull String name) {
    super();

    this.language = lang;
    this.parent = parent;
    this.name = name;
    if (parent != null) {
      this.path = parent.path + '.' + name;
      this.parent.addPackage(this);
    } else {
      this.path = name;
    }
    this.target = null;
  }

  JavaPackage(
      @NotNull JavaLanguage lang,
      @Nullable JavaPackage parent,
      @NotNull String name,
      @NotNull Map<String, Object> raw) {
    super();

    this.language = lang;
    this.parent = parent;
    this.name = name;
    if (parent != null) {
      this.path = parent.path + '.' + name;
      this.parent.addPackage(this);
    } else {
      this.path = name;
    }

    // Attempt to resolve reflection before loading.
    this.target = resolve(this.path);

    onLoad(raw);
  }

  @Override
  public boolean onCompile() {

    // Compile class(es).
    if (!classes.isEmpty()) {
      List<String> keys = new ArrayList<>(classes.keySet());
      keys.sort(Comparator.naturalOrder());

      for (String key : keys) {
        JavaClass javaClass = classes.get(key);
        if (javaClass.isDirty() && !javaClass.compile()) {
          return false;
        }
      }
    }

    // Compile sub-package(s).
    if (!packages.isEmpty()) {
      List<String> keys = new ArrayList<>(packages.keySet());
      keys.sort(Comparator.naturalOrder());

      for (String key : keys) {
        JavaPackage javaPackage = packages.get(key);
        if (javaPackage.isDirty() && !javaPackage.compile()) {
          return false;
        }
      }
    }

    return true;
  }

  protected void onLoad(@NotNull Map<String, Object> raw) {
    // TODO: Load classes.
    // TODO: Load sub-packages.
  }

  @NotNull
  protected Map<String, Object> onSave(boolean deep) {
    final Map<String, Object> raw = new HashMap<>();

    if (hasNotes()) {
      raw.put("notes", getNotes());
    }

    if (hasTags()) {
      raw.put("tags", getTags());
    }

    if (deep && hasPackages()) {
      final Map<String, Object> packages = new HashMap<>();

      // Go through each package alphanumerically.
      final List<String> keys = new ArrayList<>(this.packages.keySet());
      keys.sort(Comparator.naturalOrder());

      for (String key : keys) {
        JavaPackage javaPackage = this.packages.get(key);
        packages.put(key, javaPackage.onSave(deep));
      }

      raw.put("packages", packages);
    }

    if (hasClasses()) {
      final Map<String, Object> classes = new HashMap<>();

      // Go through each class alphanumerically.
      final List<String> keys = new ArrayList<>(this.classes.keySet());
      keys.sort(Comparator.naturalOrder());

      for (String key : keys) {
        JavaClass javaClass = this.classes.get(key);
        classes.put(key, javaClass.onSave());
      }

      raw.put("classes", classes);
    }

    return raw;
  }

  private boolean hasClasses() {
    return !this.classes.isEmpty();
  }

  boolean hasPackages() {
    return !this.packages.isEmpty();
  }

  /**
   * @return Returns a read-only map of the classes in the package.
   */
  @NotNull
  public Map<String, JavaClass> getClasses() {
    return Collections.unmodifiableMap(classes);
  }

  /**
   * @return Returns a read-only map of the sub-packages in the package.
   */
  @NotNull
  public Map<String, JavaPackage> getPackages() {
    return Collections.unmodifiableMap(packages);
  }

  /**
   * @param pkg The sub-package definition to register.
   * @throws IllegalArgumentException If the sub-package definition is already registered.
   */
  public void addPackage(@NotNull JavaPackage pkg) {
    String pkgName = pkg.getName();
    if (this.packages.containsKey(pkgName)) {
      throw new IllegalArgumentException(
          "The package \"" + this.name + "\" already contains sub-package: \"" + pkgName + "\"");
    }
    this.packages.put(pkg.getName(), pkg);
  }

  /**
   * @param pkg The sub-package definition to unregister.
   * @throws IllegalArgumentException If the sub-package definition is NOT registered.
   */
  public void removePackage(@NotNull JavaPackage pkg) {
    String pkgName = pkg.getName();
    if (!this.packages.containsKey(pkgName)) {
      throw new IllegalArgumentException(
          "The package \"" + this.name + "\" doesn't contain sub-package: \"" + pkgName + "\"");
    }
    this.packages.remove(pkgName);
  }

  /**
   * @param pkgName The name of the sub-package definition to unregister. (Case-Sensitive)
   * @return The unregistered sub-package definition.
   * @throws IllegalArgumentException If the sub-package definition is NOT registered.
   */
  public JavaPackage removePackage(@NotNull String pkgName) {
    if (!this.packages.containsKey(pkgName)) {
      throw new IllegalArgumentException(
          "The package \"" + this.name + "\" doesn't contain sub-package: \"" + pkgName + "\"");
    }
    return this.packages.remove(pkgName);
  }

  /**
   * @param pkg The sub-package definition.
   * @return True if the sub-package definition exists AND is registered.
   */
  public boolean hasPackage(@NotNull JavaPackage pkg) {
    return this.packages.containsKey(pkg.getName());
  }

  /**
   * @param pkgName The name of the package. (Case-Sensitive)
   * @return True if the sub-package definition exists AND is registered.
   */
  public boolean hasPackage(@NotNull String pkgName) {
    return this.packages.containsKey(pkgName);
  }

  /**
   * @param pkgName The name of the package. (Case-Sensitive)
   * @return The sub-package definition.
   * @throws NullPointerException If the sub-package definition doesn't exist.
   */
  @NotNull
  public JavaPackage getPackage(@NotNull String pkgName) {
    if (!this.packages.containsKey(pkgName)) {
      throw new NullPointerException(
          "Package \"" + this.name + "\" doesn't have the sub-package: \"" + pkgName + "\"");
    }
    return this.packages.get(pkgName);
  }

  /**
   * @param clazz The class definition.
   * @return True if the class definition exists AND is registered.
   */
  public boolean hasClazz(@NotNull JavaClass clazz) {
    return this.classes.containsKey(clazz.getName());
  }

  /**
   * @param clazzName The name of the class definition.
   * @return True if a class definition with the name exists AND is registered.
   */
  public boolean hasClazz(@NotNull String clazzName) {
    return this.classes.containsKey(clazzName);
  }

  /**
   * @param clazzName The name of the class. (Case-Sensitive)
   * @return The class definition.
   * @throws NullPointerException If the class definition doesn't exist.
   */
  @NotNull
  public JavaClass getClazz(@NotNull String clazzName) {
    if (!this.classes.containsKey(clazzName)) {
      throw new NullPointerException(
          "Package \"" + this.name + "\" doesn't have the class: \"" + clazzName + "\"");
    }
    return this.classes.get(clazzName);
  }

  /**
   * @param clazz The class definition to register.
   * @throws IllegalArgumentException If the class definition is already registered.
   */
  public void addClass(@NotNull JavaClass clazz) {
    String clazzName = clazz.getName();
    if (this.classes.containsKey(clazzName)) {
      throw new IllegalArgumentException(
          "The package \"" + this.name + "\" already contains class: \"" + clazzName + "\"");
    }
    this.classes.put(clazzName, clazz);
  }

  /**
   * @param clazz The class definition to unregister.
   * @throws IllegalArgumentException If the class definition is NOT registered.
   */
  public void removeClazz(@NotNull JavaClass clazz) {
    String clazzName = clazz.getName();
    if (!this.classes.containsKey(clazzName)) {
      throw new IllegalArgumentException(
          "The package \"" + this.name + "\" doesn't contain class: \"" + clazzName + "\"");
    }
    this.classes.remove(clazzName);
  }

  /**
   * @param clazzName The name of the class definition to unregister. (Case-Sensitive)
   * @return The unregistered class definition.
   * @throws IllegalArgumentException If the class definition is NOT registered.
   */
  public JavaClass removeClazz(@NotNull String clazzName) {
    if (!this.classes.containsKey(clazzName)) {
      throw new IllegalArgumentException(
          "The package \"" + this.name + "\" doesn't contain class: \"" + clazzName + "\"");
    }
    return this.classes.remove(clazzName);
  }

  public boolean canSave() {
    return hasClasses() || hasTags() || hasNotes();
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

  @NotNull
  @Override
  public Package getReflectionTarget() {
    return target;
  }

  void setReflectionTarget(@Nullable Package target) {
    this.target = target;
  }

  @Override
  public String toString() {
    return "JavaPackage \"" + getPath() + "\"";
  }

  @NotNull
  @Override
  public String getName() {
    return name;
  }

  @NotNull
  public String getPath() {
    return path;
  }

  @NotNull
  public static String popPackagePath(@NotNull String path) {

    // Make sure the package has a delimiter to grab a parent package.
    if (!path.contains(".")) {
      throw new IllegalArgumentException(
          "Package path doesn't contain delimiter '.': \"" + path + "\"");
    }

    // Optimally rebuild string using arrays, popping the pseudo-stack.
    String[] stack = path.split("\\.");
    StringBuilder built = new StringBuilder(stack[0]);
    if (stack.length > 2) {
      for (int index = 1; index < stack.length - 1; index++) {
        built.append('.').append(stack[index]);
      }
    }

    return built.toString();
  }

  /**
   * Tests if a Java-package name is valid.
   *
   * @param name The name to test.
   * @return True if the package name is valid.
   */
  public static boolean isValidName(@NotNull String name) {
    return !name.isEmpty() && REGEX_PKG_NAME.matcher(name).find();
  }

  @Nullable
  public static Package getSuperPackage(@NotNull Package pkg) {
    return getSuperPackage(pkg.getName());
  }

  @Nullable
  public static Package getSuperPackage(@NotNull Package pkg, @NotNull ClassLoader classLoader) {
    return getSuperPackage(pkg.getName(), classLoader);
  }

  /**
   * Resolves a super-package from a child package-path.
   *
   * @param path The path to the child-package.
   * @return The super-package
   */
  @Nullable
  public static Package getSuperPackage(@NotNull String path) {
    return getSuperPackage(path, ClassLoader.getSystemClassLoader());
  }

  /**
   * @param path The path to the child-package.
   * @param classLoader The classLoader storing the package.
   * @return The super-package
   */
  @Nullable
  public static Package getSuperPackage(@NotNull String path, @NotNull ClassLoader classLoader) {
    return resolve(popPackagePath(path), classLoader);
  }

  @Nullable
  public static Package resolve(@NotNull String path) {
    return resolve(path, ClassLoader.getSystemClassLoader());
  }

  @Nullable
  public static Package resolve(@NotNull String path, @NotNull ClassLoader classLoader) {
    return classLoader.getDefinedPackage(path);
  }

  public boolean hasParent() {
    return this.parent != null;
  }
}
