package com.asledgehammer.rosetta.java;

import com.asledgehammer.rosetta.NamedEntity;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class JavaExecutableCollection<E extends JavaExecutable<?>> implements NamedEntity {

  private final List<E> executables = new ArrayList<>();
  private final String name;

  JavaExecutableCollection(@NotNull String name) {
    if (!JavaExecutable.isValidName(name)) {
      throw new IllegalArgumentException(
          "The name is not a valid executable name. (Given: \"" + name + "\")");
    }
    this.name = name;
  }

  /**
   * @param executable The executable definition to test.
   * @return True if the executable definition is registered in the list.
   */
  public boolean hasExecutable(@NotNull E executable) {
    return executables.contains(executable);
  }

  /**
   * @param executable The executable definition to register to the list.
   */
  public void addExecutable(@NotNull E executable) {
    if (executables.contains(executable)) {
      throw new IllegalArgumentException(
          "The "
              + executable.getClass().getSimpleName()
              + " is already registered in the list: "
              + executable.getSignature());
    }
    executables.add(executable);
  }

  /**
   * @param executable The executable definition to unregister from the list.
   */
  public void removeExecutable(@NotNull E executable) {
    if (!executables.contains(executable)) {
      throw new IllegalArgumentException(
          "The "
              + executable.getClass().getSimpleName()
              + " is NOT registered in the list: "
              + executable.getSignature());
    }
    executables.remove(executable);
  }

  /**
   * @return A list of all executable members.
   */
  @NotNull
  public List<E> getExecutables() {
    return Collections.unmodifiableList(this.executables);
  }

  @Override
  public @NotNull String getName() {
    return this.name;
  }

  @NotNull
  public E getExecutable(@NotNull Executable executable) {
    if (executables.isEmpty()) {
      throw new NullPointerException(
          executable.getClass().getSimpleName() + " isn't registered in group.");
    }

    for (E e : executables) {
      if (e.getReflectionTarget().equals(executable)) {
        return e;
      }
    }

    throw new NullPointerException(
        executable.getClass().getSimpleName() + " isn't registered in group.");
  }

  public boolean isEmpty() {
    return executables.isEmpty();
  }
}
