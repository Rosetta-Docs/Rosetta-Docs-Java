package com.asledgehammer.rosetta;

import com.asledgehammer.rosetta.exception.MissingKeyException;
import com.asledgehammer.rosetta.exception.TypeException;
import com.asledgehammer.rosetta.exception.ValueTypeException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** RosettaObject is a common super-class for dictionary objects. */
public abstract class RosettaObject {

  /** Generic creation constructor. No arguments are passed. */
  protected RosettaObject() {}

  @Nullable
  @SuppressWarnings("unchecked")
  protected <E> E getOptionalValue(
      @NotNull Map<String, Object> raw,
      @NotNull String label,
      @NotNull String key,
      @NotNull Class<E> type) {

    if (!raw.containsKey(key)) {
      return null;
    }

    Object oValue = raw.get(key);
    Class<?> oClazz = oValue.getClass();
    if (!oValue.equals(type) && !type.isAssignableFrom(oClazz)) {
      throw new ValueTypeException(label, key, oValue.getClass(), type);
    }

    return (E) oValue;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  protected Map<String, Object> getOptionalDictionary(
      @NotNull Map<String, Object> raw, @NotNull String label, @NotNull String key) {
    if (!raw.containsKey(key)) return null;
    return getExpectedValue(raw, label, key, Map.class);
  }

  @Nullable
  @SuppressWarnings("unchecked")
  protected List<Object> getOptionalList(
      @NotNull Map<String, Object> raw,
      @NotNull String label,
      @NotNull String key,
      @NotNull Class<?>... types) {

    if (!raw.containsKey(key)) {
      return null;
    }

    final List<Object> list = getExpectedValue(raw, label, key, List.class);
    for (int i = 0; i < list.size(); i++) {
      Class<?> entryClass = list.get(i).getClass();
      boolean valid = false;
      for (Class<?> type : types) {
        if (type.isAssignableFrom(entryClass)) {
          valid = true;
          break;
        }
      }
      if (!valid) {
        throw new TypeException(label + "[" + i + "]", entryClass, types);
      }
    }

    return list;
  }

  @Nullable
  protected List<String> getOptionalStringList(
      @NotNull Map<String, Object> raw, @NotNull String label, @NotNull String key) {

    if (!raw.containsKey(key)) {
      return null;
    }

    final List<String> list = new ArrayList<>();

    List<?> oValue = getExpectedValue(raw, label, key, List.class);
    for (int i = 0; i < oValue.size(); i++) {
      Object oEntry = oValue.get(i);
      if (!(oEntry instanceof String)) {
        throw new TypeException(label + "[" + i + "]", oEntry.getClass(), String.class);
      }
      list.add((String) oEntry);
    }

    return list;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  protected List<Map<String, Object>> getOptionalDictionaryList(
      @NotNull Map<String, Object> raw, @NotNull String label, @NotNull String key) {

    if (!raw.containsKey(key)) {
      return null;
    }

    final List<Map<String, Object>> list = new ArrayList<>();

    List<?> oValue = getExpectedValue(raw, label, key, List.class);
    for (int i = 0; i < oValue.size(); i++) {
      Object oEntry = oValue.get(i);
      if (!(oEntry instanceof Map)) {
        throw new TypeException(label + "[" + i + "]", oEntry.getClass(), Map.class);
      }
      list.add((Map<String, Object>) oEntry);
    }

    return list;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  protected <E> E getOptionalValue(
      @NotNull Map<String, Object> raw,
      @NotNull String label,
      @NotNull String key,
      @NotNull E defaultValue,
      @NotNull Class<E> type) {

    if (!raw.containsKey(key)) {
      return defaultValue;
    }

    @NotNull Object oValue = raw.get(key);
    Class<?> oClazz = oValue.getClass();
    if (!oValue.equals(type) && !type.isAssignableFrom(oClazz)) {
      throw new ValueTypeException(label, key, oValue.getClass(), type);
    }

    return (E) oValue;
  }

  @Nullable
  protected Object getOptionalValue(
      @NotNull Map<String, Object> raw,
      @NotNull String label,
      @NotNull String key,
      @NotNull Class<?>... types) {

    if (!raw.containsKey(key)) {
      return null;
    }

    Object oValue = raw.get(key);
    Class<?> oClazz = oValue.getClass();

    for (Class<?> type : types) {
      if (oValue.equals(type) || type.isAssignableFrom(oClazz)) {
        return oValue;
      }
    }

    throw new ValueTypeException(label, key, oClazz, types);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  protected Map<String, Object> getExpectedDictionary(
      @NotNull Map<String, Object> raw, @NotNull String label, @NotNull String key) {
    return getExpectedValue(raw, label, key, Map.class);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  protected <E> E getExpectedValue(
      @NotNull Map<String, Object> raw,
      @NotNull String label,
      @NotNull String key,
      @NotNull Class<E> type) {

    if (!raw.containsKey(key)) {
      throw new MissingKeyException(label, key);
    }

    Object oValue = raw.get(key);
    Class<?> oClazz = oValue.getClass();
    if (!oValue.equals(type) && !type.isAssignableFrom(oClazz)) {
      throw new ValueTypeException(label, key, oValue.getClass(), type);
    }

    return (E) oValue;
  }

  @NotNull
  protected Object getExpectedValue(
      @NotNull Map<String, Object> raw,
      @NotNull String label,
      @NotNull String key,
      @NotNull Class<?>... types) {

    if (!raw.containsKey(key)) {
      throw new MissingKeyException(label, key);
    }

    Object oValue = raw.get(key);
    Class<?> oClazz = oValue.getClass();

    for (Class<?> type : types) {
      if (oValue.equals(type) || type.isAssignableFrom(oClazz)) {
        return oValue;
      }
    }

    throw new ValueTypeException(label, key, oClazz, types);
  }
}
