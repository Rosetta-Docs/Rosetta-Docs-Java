package com.asledgehammer.rosetta.java.reference;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class SimpleTypeReference extends TypeReference {

  static final TypeReference[] OBJECT_BOUNDS;

  static {
    OBJECT_BOUNDS = new TypeReference[] {TypeReference.of(Object.class)};
  }

  private final List<TypeReference> subTypes;
  private final String base;
  private final boolean wildcard;
  private final boolean primitive;
  private final boolean generic;
  private final boolean array;
  private final int arrayDepth;
  private final TypeReference[] bounds;

  SimpleTypeReference(@NotNull String raw) {
    raw = raw.trim();

    // If the type is an array, parse out the syntax and calculate its depth.
    if (raw.endsWith("[]")) {
      this.array = true;
      int arrayDepth = 0;
      while (raw.endsWith("[]")) {
        arrayDepth++;
        raw = raw.substring(0, raw.length() - 2);
      }
      this.arrayDepth = arrayDepth;
    } else {
      this.array = false;
      this.arrayDepth = 0;
    }

    final int firstIndexOfExtends = raw.indexOf(" extends ");
    final int firstIndexOfSuper = raw.indexOf(" super ");
    final int firstIndexOfSubTypes = raw.indexOf("<");

    // Check if the subtypes are defined for the main type provided, not a subtype that follows
    // 'extends' or
    // 'super of' rules.
    if (firstIndexOfSubTypes != -1
        && (firstIndexOfExtends == -1 || firstIndexOfSubTypes < firstIndexOfExtends)
        && (firstIndexOfSuper == -1 || firstIndexOfSubTypes < firstIndexOfSuper)) {
      this.base = raw.substring(0, raw.indexOf('<'));
      List<String> subTypesStr = getGenericTypes(raw);
      subTypes = new ArrayList<>();
      for (String subTypeStr : subTypesStr) {
        subTypes.add(TypeReference.of(subTypeStr));
      }
    } else {
      String base = raw.trim();

      if (firstIndexOfExtends != -1 && firstIndexOfSuper != 1) {
        if (firstIndexOfExtends < firstIndexOfSuper) {
          this.base = base.split(" extends ")[0];
        } else {
          this.base = base.split(" super ")[0];
        }
      } else if (firstIndexOfExtends != -1) {
        this.base = base.split(" extends ")[0];
      } else if (firstIndexOfSuper != -1) {
        this.base = base.split(" super ")[0];
      } else {
        this.base = base;
      }
      this.subTypes = null;
    }

    this.wildcard = this.base.equals("?");
    this.primitive = PRIMITIVE_TYPES.contains(this.base);
    boolean generic = this.wildcard;
    if (!generic && !this.primitive) {
      // Attempt to resolve the path. if it doesn't exist then it's considered generic.
      try {
        Class.forName(this.base, false, ClassLoader.getSystemClassLoader());
      } catch (Exception e) {
        generic = true;
      }
    }
    this.generic = generic;
    this.bounds = this.generic ? OBJECT_BOUNDS : new TypeReference[] {this};
  }

  @NotNull
  public String compile() {
    String compiled = this.base;
    if (subTypes != null) {
      StringBuilder subTypeStr = new StringBuilder();
      for (TypeReference subType : subTypes) {
        if (subTypeStr.isEmpty()) {
          subTypeStr = new StringBuilder(subType.compile());
        } else {
          subTypeStr.append(", ").append(subType.compile());
        }
      }
      compiled += '<' + subTypeStr.toString() + '>';
    }
    return compiled;
  }

  @NotNull
  public String compile(@NotNull ClassReference reference, @NotNull Class<?> deCl) {
    String compiled = this.base;
    if (subTypes != null) {
      StringBuilder subTypeStr = new StringBuilder();
      for (TypeReference subType : subTypes) {
        if (subTypeStr.isEmpty()) {
          subTypeStr = new StringBuilder(subType.compile(reference, deCl));
        } else {
          subTypeStr.append(", ").append(subType.compile(reference, deCl));
        }
      }
      compiled += '<' + subTypeStr.toString() + '>';
    }
    return compiled;
  }

  @Override
  public boolean isWildcard() {
    return wildcard;
  }

  @Override
  public boolean isGeneric() {
    return generic;
  }

  public boolean isArray() {
    return array;
  }

  public int getArrayDepth() {
    return arrayDepth;
  }

  @NotNull
  @Override
  public String getBase() {
    return this.base;
  }

  public boolean isPrimitive() {
    return primitive;
  }

  @NotNull
  @Override
  public TypeReference[] getBounds() {
    return bounds;
  }

  public boolean hasSubTypes() {
    return this.subTypes != null && !this.subTypes.isEmpty();
  }

  /**
   * @return The subTypes list.
   * @throws NullPointerException If the subTypes is null. (use {@link
   *     SimpleTypeReference#hasSubTypes()})
   */
  @NotNull
  public List<TypeReference> getSubTypes() {
    if (this.subTypes == null) {
      throw new NullPointerException("The type does not have sub-types.");
    }
    return subTypes;
  }

  public static List<String> getGenericTypes(String raw) {
    int level = 0;
    final List<String> vars = new ArrayList<>();
    StringBuilder var = new StringBuilder();
    for (int x = 0; x < raw.length(); x++) {
      char curr = raw.charAt(x);
      if (curr == '<') {
        level++;
        if (level > 1) {
          var.append(curr);
        }
      } else if (curr == '>') {
        level--;
        if (level > 0) {
          var.append(curr);
        } else {
          break;
        }
      } else if (curr == ',' && level == 1) {
        vars.add(var.toString().trim());
        var = new StringBuilder();
      } else if (level != 0) {
        var.append(curr);
      }
    }

    if (!var.isEmpty()) {
      vars.add(var.toString().trim());
    }

    return vars;
  }
}
