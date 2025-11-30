package com.asledgehammer.rosetta.java;

import com.asledgehammer.rosetta.RosettaLanguage;
import com.asledgehammer.rosetta.exception.MissingKeyException;
import com.asledgehammer.rosetta.exception.RosettaException;
import com.asledgehammer.rosetta.exception.TypeException;
import com.asledgehammer.rosetta.exception.ValueTypeException;
import com.asledgehammer.rosetta.java.reference.ClassReference;
import com.asledgehammer.rosetta.java.reference.SimpleTypeReference;
import com.asledgehammer.rosetta.java.reference.TypeReference;
import com.asledgehammer.rosetta.java.reference.UnionTypeReference;
import java.lang.reflect.*;
import java.util.*;
import org.jetbrains.annotations.NotNull;

public class JavaLanguage
    implements RosettaLanguage<JavaSerializeSettings, JavaDeserializeSettings> {

  final Map<String, JavaClass> classes = new HashMap<>();
  final Map<String, JavaPackage> packages = new HashMap<>();

  public JavaLanguage() {}

  @NotNull
  public JavaPackage of(@NotNull Package pkg) {

    String path = pkg.getName();
    if (packages.containsKey(path)) {
      return packages.get(path);
    }

    JavaPackage parent = null;
    if (path.contains(".")) {
      // Recursively resolve upward the parent & link to the base package object.
      String[] split = path.split("\\.");
      if (split.length == 2) {
        Package pkgParent = JavaPackage.resolve(split[0]);
        if (pkgParent != null) {
          parent = of(pkgParent);
        } else {
          parent = ofInternalPackage(split[0]);
        }
      } else {
        StringBuilder join = new StringBuilder(split[0]);
        for (int i = 1; i < split.length - 1; i++) {
          join.append(".").append(split[i]);
        }
        Package pkgParent = JavaPackage.resolve(join.toString());
        if (pkgParent != null) {
          parent = of(pkgParent);
        } else {
          parent = ofInternalPackage(join.toString());
        }
      }
    }

    // Create & cache the package definition.
    JavaPackage javaPackage = new JavaPackage(this, parent, pkg);
    packages.put(path, javaPackage);

    return javaPackage;
  }

  @NotNull
  private JavaPackage ofInternalPackage(@NotNull String path) {

    if (this.packages.containsKey(path)) {
      return this.packages.get(path);
    }

    JavaPackage parent = null;
    String name;
    if (path.contains(".")) {
      String[] split = path.split("\\.");
      name = split[split.length - 1];

      if (split.length == 2) {
        Package pkgParent = JavaPackage.resolve(split[0]);
        if (pkgParent != null) {
          parent = of(pkgParent);
        } else {
          parent = ofInternalPackage(split[0]);
        }
      } else {
        StringBuilder join = new StringBuilder(split[0]);
        for (int i = 1; i < split.length - 1; i++) {
          join.append(".").append(split[i]);
        }
        Package pkgParent = JavaPackage.resolve(join.toString());
        if (pkgParent != null) {
          parent = of(pkgParent);
        } else {
          parent = ofInternalPackage(join.toString());
        }
      }
    } else {
      name = path;
    }

    JavaPackage javaPackage = new JavaPackage(this, parent, name);
    this.packages.put(name, javaPackage);
    return javaPackage;
  }

  @NotNull
  public JavaClass of(@NotNull JavaDiscoverySettings settings, @NotNull Class<?> clazz) {

    String qualifiedPath = clazz.getName();
    if (classes.containsKey(qualifiedPath)) {
      return classes.get(qualifiedPath);
    }

    // Create & cache the class definition.
    JavaPackage javaPackage = of(clazz.getPackage());
    JavaClass javaClass = new JavaClass(javaPackage, clazz);

    classes.put(qualifiedPath, javaClass);

    // IMPORTANT: Discover post-constructor to prevent null-referencing if self-referencing on
    // discovery.
    javaClass.discover(settings, clazz, this);

    // If the class is enclosed, reference the class in there instead of the package to properly
    // reflect its path.
    Class<?> clazzEnclosing = clazz.getEnclosingClass();
    if (clazzEnclosing != null) {
      of(settings, clazzEnclosing).addClass(javaClass);
    } else {
      javaPackage.addClass(javaClass);
    }

    return javaClass;
  }

  @NotNull
  public JavaMethod of(@NotNull JavaDiscoverySettings settings, @NotNull Method method) {
    return of(settings, method.getDeclaringClass()).getMethod(method);
  }

  @Override
  @SuppressWarnings({"unchecked"})
  public void onLoad(
      @NotNull JavaDeserializeSettings settings,
      @NotNull String id,
      @NotNull Map<String, Object> raw) {

    // No Java packages? Return.
    if (!raw.containsKey("packages")) return;

    final Object oPackages = raw.get("packages");
    if (!(oPackages instanceof Map)) {
      throw new RosettaException("The property \"languages.java.packages\" is not a dictionary.");
    }
    final Map<String, Object> packages = (Map<String, Object>) oPackages;

    final List<String> keys = new ArrayList<>(packages.keySet());
    keys.sort(Comparator.naturalOrder());
    for (String key : keys) {
      final Object oPackage = packages.get(key);
      if (!(oPackage instanceof Map)) {
        throw new RosettaException(
            "The property \"languages.java.packages." + key + "\" is not a dictionary.");
      }
      JavaPackage javaPackage = new JavaPackage(this, null, key, (Map<String, Object>) oPackage);
      packages.put(javaPackage.getName(), javaPackage);
    }
  }

  private void onSavePackageInternal(
      @NotNull JavaSerializeInstance serialize,
      @NotNull Map<String, Object> raw,
      @NotNull JavaPackage javaPackage) {

    // Only save packages that contains contents.
    if (javaPackage.canSave()) {
      raw.put(javaPackage.getPath(), javaPackage.onSave(false, serialize));
    }

    // Use this to flatten packages to prevent unnecessary nesting.
    if (javaPackage.hasPackages()) {
      Map<String, JavaPackage> packages = javaPackage.getPackages();
      List<String> keys = new ArrayList<>(packages.keySet());
      keys.sort(Comparator.naturalOrder());
      for (String key : keys) {
        onSavePackageInternal(serialize, raw, packages.get(key));
      }
    }
  }

  @NotNull
  @Override
  public Map<String, Object> onSave(@NotNull JavaSerializeSettings settings, @NotNull String id) {

    final JavaSerializeInstance serialize = new JavaSerializeInstance(settings, id);
    final Map<String, Object> raw = new TreeMap<>();

    if (hasPackages()) {
      final Map<String, Object> packages = new TreeMap<>();

      // Go through each package alphanumerically.
      final List<String> keys = new ArrayList<>(this.packages.keySet());
      keys.sort(Comparator.naturalOrder());

      for (String key : keys) {
        final JavaPackage javaPackage = this.packages.get(key);
        if (!javaPackage.hasParent()) {
          onSavePackageInternal(serialize, packages, javaPackage);
        }
      }

      if (!packages.isEmpty()) {
        raw.put("packages", packages);
      }
    }

    // If using a type-dictionary, render it last to let registrations happen first.
    if (serialize.hasTypeDictionary()) {
      Map<String, Object> types = new TreeMap<>();
      serialize.getTypeDictionary().render(types);
      raw.put("types", types);
    }

    return raw;
  }

  private boolean hasPackages() {
    return !this.packages.isEmpty();
  }

  @NotNull
  @Override
  public String getID() {
    return "java";
  }

  /**
   * Resolves a TypeReference from Rosetta-defined data.
   *
   * @param oType Either a String or a Map.
   * @return A built type-reference.
   */
  @SuppressWarnings("unchecked")
  public static TypeReference resolveType(@NotNull Object oType) {

    if (oType instanceof String) {
      return TypeReference.of((String) oType);
    } else if (!(oType instanceof Map)) {
      throw new TypeException("type", oType.getClass(), String.class, Map.class);
    }

    Map<String, Object> map = (Map<String, Object>) oType;

    // Retrieve the base string.
    Object oBase = map.get("base");
    if (oBase == null) {
      throw new MissingKeyException("type", "base");
    } else if (!(oBase instanceof String)) {
      throw new ValueTypeException("type", "base", oBase.getClass(), String.class);
    }

    String full = (String) oBase;
    // Retrieve any parameters defined.
    if (map.containsKey("parameters")) {
      Object oParameters = map.get("parameters");
      if (!(oParameters instanceof List)) {
        throw new ValueTypeException("type", "parameters", oParameters.getClass(), List.class);
      }

      StringBuilder sub = new StringBuilder();
      for (Object oParameter : (List<?>) oParameters) {
        if (sub.isEmpty()) {
          sub.append(resolveType(oParameter).getBase());
        } else {
          sub.append(", ").append(resolveType(oParameter).getBase());
        }
      }
      full += "<" + sub + ">";
    }

    return TypeReference.of(full);
  }

  /**
   * @param type The type to serialize.
   * @return Either a {@link String} for {@link SimpleTypeReference without {@link
   *     SimpleTypeReference#hasSubTypes()} being true} or a {@link Map}.
   */
  @NotNull
  public static Object serializeType(
      @NotNull JavaSerializeInstance serialize,
      @NotNull TypeReference type,
      @NotNull ClassReference reference,
      @NotNull Class<?> deCl) {
    Map<String, Object> raw;
    if (type instanceof SimpleTypeReference simple) {

      // The most simple form of a type. Only serialize the base string.
      if (!simple.hasSubTypes() && !simple.isGeneric()) {
        return type.compile(reference, deCl);
      }

      raw = new TreeMap<>();
      if (serialize.isWriteFullType()) {
        raw.put("full", simple.compile(reference, deCl));
      }
      raw.put("base", simple.getBase());
      if (type.isGeneric()) {
        raw.put("generic", true);
      }

      if (simple.hasSubTypes()) {
        List<Object> parameters = new ArrayList<>();
        if (serialize.hasTypeDictionary()) {
          final JavaTypeDictionary typeDictionary = serialize.getTypeDictionary();
          for (TypeReference subType : simple.getSubTypes()) {
            parameters.add(typeDictionary.register(serialize, subType, reference, deCl));
          }
        } else {
          for (TypeReference subType : simple.getSubTypes()) {
            parameters.add(serializeType(serialize, subType, reference, deCl));
          }
        }
        raw.put("parameters", parameters);
      }
    } else {
      UnionTypeReference union = (UnionTypeReference) type;
      raw = new TreeMap<>();
      raw.put("full", union.compile(reference, deCl));
      raw.put("base", union.getBase());
      raw.put("generic", union.isGeneric());
      TypeReference[] trBounds = union.getBounds();
      if (trBounds != null) {
        raw.put("bounds_type", union.isExtendsOrSuper() ? "extends" : "super");
        List<Object> bounds = new ArrayList<>();
        if (serialize.hasTypeDictionary()) {
          final JavaTypeDictionary typeDictionary = serialize.getTypeDictionary();
          for (TypeReference bound : trBounds) {
            bounds.add(typeDictionary.register(serialize, bound, reference, deCl));
          }
        } else {
          for (TypeReference bound : trBounds) {
            bounds.add(serializeType(serialize, bound, reference, deCl));
          }
        }
        raw.put("bounds", bounds);
      }
    }
    return raw;
  }

  @NotNull
  public static JavaScope getScope(@NotNull Class<?> clazz) {
    return getScope(clazz.getModifiers());
  }

  @NotNull
  public static JavaScope getScope(@NotNull Executable executable) {
    return getScope(executable.getModifiers());
  }

  @NotNull
  public static JavaScope getScope(@NotNull Parameter parameter) {
    return getScope(parameter.getModifiers());
  }

  @NotNull
  public static JavaScope getScope(@NotNull Field field) {
    return getScope(field.getModifiers());
  }

  /**
   * Resolves the scope of a Java reflection target.
   *
   * @param modifiers {@link Class#getModifiers()}, {@link Executable#getModifiers()}, {@link
   *     Parameter#getModifiers()}, or {@link Field#getModifiers()}.
   * @return The scope of the Java reflection target.
   */
  @NotNull
  static JavaScope getScope(int modifiers) {
    if (Modifier.isPublic(modifiers)) {
      return JavaScope.PUBLIC;
    } else if (Modifier.isProtected(modifiers)) {
      return JavaScope.PROTECTED;
    } else if (Modifier.isPrivate(modifiers)) {
      return JavaScope.PRIVATE;
    } else {
      return JavaScope.PACKAGE;
    }
  }

  public static boolean isStatic(@NotNull Class<?> clazz) {
    return Modifier.isStatic(clazz.getModifiers());
  }

  public static boolean isStatic(@NotNull Method method) {
    return Modifier.isStatic(method.getModifiers());
  }

  public static boolean isStatic(@NotNull Field field) {
    return Modifier.isStatic(field.getModifiers());
  }

  public static boolean isStatic(int modifiers) {
    return Modifier.isStatic(modifiers);
  }

  public static boolean isFinal(@NotNull Method method) {
    return Modifier.isFinal(method.getModifiers());
  }

  public static boolean isFinal(@NotNull Parameter parameter) {
    return Modifier.isFinal(parameter.getModifiers());
  }

  public static boolean isFinal(@NotNull Class<?> clazz) {
    return Modifier.isFinal(clazz.getModifiers());
  }

  public static boolean isFinal(@NotNull Field field) {
    return Modifier.isFinal(field.getModifiers());
  }

  public static boolean isFinal(int modifiers) {
    return Modifier.isFinal(modifiers);
  }

  public static boolean isTransient(@NotNull Field field) {
    return Modifier.isTransient(field.getModifiers());
  }

  public static boolean isTransient(int modifiers) {
    return Modifier.isTransient(modifiers);
  }

  public static boolean isVolatile(@NotNull Field field) {
    return Modifier.isVolatile(field.getModifiers());
  }

  public static boolean isVolatile(int modifiers) {
    return Modifier.isVolatile(modifiers);
  }

  public static boolean isNative(@NotNull Method method) {
    return Modifier.isNative(method.getModifiers());
  }

  public static boolean isNative(int modifiers) {
    return Modifier.isNative(modifiers);
  }
}
