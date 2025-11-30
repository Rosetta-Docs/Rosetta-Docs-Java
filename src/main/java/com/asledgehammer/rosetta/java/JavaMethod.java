package com.asledgehammer.rosetta.java;

import com.asledgehammer.rosetta.Taggable;
import com.asledgehammer.rosetta.exception.ValueTypeException;
import com.asledgehammer.rosetta.java.reference.ClassReference;
import java.lang.reflect.Method;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaMethod extends JavaExecutable<Method> implements Taggable {

  @Nullable private JavaReturn returns;

  private final List<String> tags = new ArrayList<>();

  JavaMethod(@NotNull Method method) {
    super(method);

    // TODO: Implement discovery.
    this.returns = new JavaReturn(method.getGenericReturnType());
  }

  JavaMethod(@NotNull String name, @NotNull Map<String, Object> raw) {
    super(name, raw);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void onLoad(@NotNull Map<String, Object> raw) {

    // Load all general executable data.
    super.onLoad(raw);

    // If return data is present, process it.
    if (raw.containsKey("return")) {
      Object oReturns = raw.get("return");
      if (!(oReturns instanceof Map)) {
        throw new ValueTypeException(name, "return", oReturns.getClass(), Map.class);
      }
      this.returns = new JavaReturn((Map<String, Object>) oReturns);
    } else {
      // Null definitions are void.
      this.returns = new JavaReturn(void.class);
    }
  }

  @NotNull
  protected Map<String, Object> onSave(
      @NotNull JavaSerializeInstance serialize, @NotNull ClassReference reference) {

    // Save the general executable definitions info first.
    Map<String, Object> raw = super.onSave(serialize, reference);

    raw.put("name", getName());

    // Save the returns definition if qualified.
    if (returns != null && returns.shouldSave()) {
      raw.put(
          "return",
          returns.onSave(serialize, reference, getReflectionTarget().getDeclaringClass()));
    }

    if (hasTags()) {
      raw.put("tags", getTags());
    }

    return raw;
  }

  @Nullable
  public JavaReturn getReturns() {
    return this.returns;
  }

  public void setReturns(@Nullable JavaReturn returns) {

    // Prevent redundant casts.
    if (this.returns == null) {
      if (returns == null) return;
    } else if (Objects.equals(returns, this.returns)) {
      return;
    }

    this.returns = returns;
  }

  @Override
  public String toString() {
    return "JavaMethod \"" + getSignature() + "\"";
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
}
