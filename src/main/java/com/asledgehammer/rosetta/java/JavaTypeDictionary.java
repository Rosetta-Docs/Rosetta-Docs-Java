package com.asledgehammer.rosetta.java;

import com.asledgehammer.rosetta.java.reference.ClassReference;
import com.asledgehammer.rosetta.java.reference.TypeReference;
import java.util.*;
import org.jetbrains.annotations.NotNull;

public class JavaTypeDictionary {

  private final Map<Integer, Object> mapRendered = new HashMap<>();
  private final Map<String, Integer> mapReference = new HashMap<>();
  private final String id;
  private final TypeDictionaryMode mode;

  /**
   * @param id The ID of the container file to reference.
   */
  JavaTypeDictionary(@NotNull TypeDictionaryMode mode, @NotNull String id) {
    this.mode = mode;
    this.id = id;
  }

  /**
   * Registers a chain reference of instance-specific type.
   *
   * @param type The type.
   * @param classReference The class the type is invoked.
   * @param deCl The declaring class of the type.
   */
  public String register(
      @NotNull JavaSerializeInstance serialize,
      @NotNull TypeReference type,
      @NotNull ClassReference classReference,
      @NotNull Class<?> deCl) {

    final String fKey = type.compile(classReference, deCl);
    if (mapReference.containsKey(fKey)) {
      return "$" + id + ":" + mapReference.get(fKey);
    }

    Object value = JavaLanguage.serializeType(serialize, type, classReference, deCl);
    int iKey = mapRendered.size();
    mapRendered.put(iKey, value);
    mapReference.put(fKey, iKey);
    return "$" + id + ":" + iKey;
  }

  @NotNull
  public Object render() {
    if (mode == TypeDictionaryMode.DICTIONARY) {
      Map<String, Object> raw = new TreeMap<>();
      List<Integer> keys = new ArrayList<>(mapRendered.keySet());
      keys.sort(Integer::compare);
      for (int key : keys) raw.put("" + key, mapRendered.get(key));
      return raw;
    } else {
      List<Object> raw = new ArrayList<>();
      List<Integer> keys = new ArrayList<>(mapRendered.keySet());
      keys.sort(Integer::compare);
      for (int key : keys) raw.add(mapRendered.get(key));
      return raw;
    }
  }

  @NotNull
  public String getId() {
    return id;
  }

  public TypeDictionaryMode getMode() {
    return mode;
  }

  public void clear() {
    mapReference.clear();
    mapRendered.clear();
  }

}
