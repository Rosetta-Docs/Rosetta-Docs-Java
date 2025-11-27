package com.asledgehammer.rosetta.java.reference;

import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class UnionTypeReference extends TypeReference implements BoundReference {

  private final TypeReference[] bounds;
  private final String base;
  private final boolean extendsOrSuper;
  private final boolean wildcard;
  private final boolean primitive;
  private final boolean generic;

  private static Map<String, Boolean> MAP_CHECKED_GENERIC = new HashMap<>();

  private static boolean isGeneric(String base) {
    if (MAP_CHECKED_GENERIC.containsKey(base)) {
      return MAP_CHECKED_GENERIC.get(base);
    }

    boolean result = false;
    // Attempt to resolve the path. if it doesn't exist then it's considered generic.
    try {
      Class.forName(base, false, ClassLoader.getSystemClassLoader());
    } catch (Exception e) {
      result = true;
    }

    MAP_CHECKED_GENERIC.put(base, result);

    return result;
  }

  UnionTypeReference(
      @NotNull String base, boolean extendsOrSuper, @Nullable TypeReference[] bounds) {

    //    System.out.println(
    //        "new UnionTypeReference(base = \""
    //            + base
    //            + "\", extendsOrSuper="
    //            + extendsOrSuper
    //            + ", bounds = "
    //            + Arrays.toString(bounds)
    //            + ")");

    base = base.trim();
    this.wildcard = base.startsWith("?");
    if (this.wildcard) {
      this.base = "?";
      this.primitive = false;
      this.generic = true;
      this.extendsOrSuper = base.contains("? extends ");

      // We parse the types because they are preserved in runtime here, otherwise we'd only see
      // Object.
      if (!base.equals("?")) {
        String sub;
        if (this.extendsOrSuper) {
          sub = base.substring("? extends ".length());
        } else {
          sub = base.substring("? super ".length());
        }

        StringBuilder currSub = new StringBuilder();
        List<TypeReference> subs = new ArrayList<>();
        int level = 0;
        for (int i = 0; i < sub.length(); i++) {
          char curr = sub.charAt(i);
          if (curr == '<') {
            level++;
          } else if (curr == '>') {
            level--;
          } else if (curr == '&' && level == 0) {
            subs.add(TypeReference.of(currSub.toString().trim()));
            currSub = new StringBuilder();
            continue;
          }
          currSub.append(curr);
        }
        if (!currSub.isEmpty()) {
          subs.add(TypeReference.of(currSub.toString().trim()));
        }
        bounds = new TypeReference[subs.size()];
        for (int i = 0; i < bounds.length; i++) {
          bounds[i] = subs.get(i);
        }
        this.bounds = bounds;
      } else {
        // Normal wildcard that allows any type of object.
        this.bounds = null;
      }
    } else {
      this.base = base.trim();
      this.extendsOrSuper = extendsOrSuper;
      this.bounds = bounds;
      String adjustedBase = this.base.replace("[]", "");
      this.primitive = PRIMITIVE_TYPES.contains(adjustedBase);
      boolean generic = false;
      if (!this.primitive) {
        generic = isGeneric(adjustedBase);
      }
      this.generic = generic;
    }
  }

  @NotNull
  @Override
  public String compile() {
    if (!isGeneric()) return this.base;
    StringBuilder builder = new StringBuilder(this.base);
    if (this.extendsOrSuper) {
      builder.append(" extends ");
    } else {
      builder.append(" super ");
    }
    if (this.bounds != null) {
      for (int i = 0; i < this.bounds.length; i++) {
        TypeReference reference = this.bounds[i];
        if (i != 0) builder.append(" & ");
        builder.append(reference.compile());
      }
    }
    return builder.toString();
  }

  @NotNull
  @Override
  public String compile(@NotNull ClassReference clazzReference, @NotNull Class<?> deCl) {

    if (this.bounds == null) return this.base;

    StringBuilder builder = new StringBuilder(this.base);
    if (this.extendsOrSuper) {
      builder.append(" extends ");
    } else {
      builder.append(" super ");
    }
    for (int i = 0; i < this.bounds.length; i++) {
      TypeReference reference = this.bounds[i];
      if (i != 0) builder.append(" & ");
      builder.append(reference.compile(clazzReference, deCl));
    }
    return builder.toString();
  }

  @Override
  public boolean isWildcard() {
    return wildcard;
  }

  @Override
  public boolean isGeneric() {
    return generic;
  }

  @NotNull
  @Override
  public String getBase() {
    return this.base;
  }

  @Override
  public boolean isPrimitive() {
    return primitive;
  }

  @NotNull
  @Override
  public TypeReference[] getBounds() {
    return bounds;
  }

  public boolean isExtendsOrSuper() {
    return extendsOrSuper;
  }
}
