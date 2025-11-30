package com.asledgehammer.rosetta.java;

import com.asledgehammer.rosetta.java.reference.ClassReference;
import com.asledgehammer.rosetta.java.reference.TypeReference;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class JavaTypeDictionary {

  private final Map<Integer, Object> mapRendered = new HashMap<>();
  private final Map<String, Integer> mapReference = new HashMap<>();
  private final String id;

  /**
   * @param id The ID of the container file to reference.
   */
  JavaTypeDictionary(@NotNull String id) {
    this.id = id;
  }

  /**
   * Registers a chain reference of instance-specific type.
   *
   * @param type The type.
   * @param classReference The class the type is invoked.
   * @param deCl The declaring class of the type.
   */
  public int register(
      @NotNull TypeReference type, @NotNull ClassReference classReference, @NotNull Class<?> deCl) {

    final String fKey = type.compile(classReference, deCl);
    if (mapReference.containsKey(fKey)) {
      return mapReference.get(fKey);
    }

    Object value = JavaLanguage.serializeType(type, classReference, deCl);
    int iKey = mapRendered.size();
    mapRendered.put(iKey, value);
    mapReference.put(fKey, iKey);
    return iKey;
  }

  /**
   * @param raw The map to store the dictionary.
   */
  public void render(@NotNull Map<String, Object> raw) {
    List<Integer> keys = new ArrayList<>(mapRendered.keySet());
    keys.sort(Comparator.naturalOrder());
    for (int key : keys) raw.put("" + key, keys.get(key));
  }

  @NotNull
  public String getId() {
    return id;
  }

  public void clear() {
    mapReference.clear();
    mapRendered.clear();
  }
}
