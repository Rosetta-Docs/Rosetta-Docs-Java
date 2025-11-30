package com.asledgehammer.rosetta;

import com.asledgehammer.rosetta.exception.UnsupportedApplicationException;
import com.asledgehammer.rosetta.exception.UnsupportedLanguageException;
import com.asledgehammer.rosetta.java.JavaLanguage;
import com.asledgehammer.rosetta.lua.LuaLanguage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;

/** Rosetta houses all general root-level operations for the Rosetta-Docs-Java framework. */
public class Rosetta {

  private static final Load DEFAULT_LOAD;
  private static final Dump DEFAULT_DUMP;

  private static final Map<String, Class<? extends RosettaApplication>> APPLICATIONS;
  private static final Map<String, Class<? extends RosettaLanguage<?, ?>>> LANGUAGES;

  static {
    LoadSettings loadSettings = LoadSettings.builder().build();
    DEFAULT_LOAD = new Load(loadSettings);
    DumpSettings dumpSettings = DumpSettings.builder().setDefaultFlowStyle(FlowStyle.BLOCK).build();
    DEFAULT_DUMP = new Dump(dumpSettings);

    LANGUAGES = new HashMap<>();
    APPLICATIONS = new HashMap<>();

    LANGUAGES.put("java", JavaLanguage.class);
    LANGUAGES.put("lua", LuaLanguage.class);
  }

  /**
   * Registers a class-type to handle all processing of Rosetta data for a application.
   *
   * @param id The YAML ID of the application.
   * @param type The class-type to instantiate.
   * @throws NullPointerException If the ID or type are null.
   */
  public static void registerApplication(
      @NotNull String id, @NotNull Class<? extends RosettaApplication> type) {
    String idLower = id.toLowerCase();
    if (APPLICATIONS.containsKey(idLower)) {
      System.out.println(
          "Warning: The RosettaApplication \""
              + idLower
              + "\" is already registered and its adapted class will be overwritten. ("
              + APPLICATIONS.get(idLower).getName()
              + " becomes "
              + type.getName()
              + ")");
    }
    APPLICATIONS.put(id, type);
  }

  /**
   * Registers a class-type to handle all processing of Rosetta data for a language.
   *
   * @param id The YAML ID of the language.
   * @param type The class-type to instantiate.
   * @throws NullPointerException If the ID or type are null.
   */
  public static void registerLanguage(
      @NotNull String id, @NotNull Class<? extends RosettaLanguage<?, ?>> type) {
    String idLower = id.toLowerCase();
    if (LANGUAGES.containsKey(idLower)) {
      System.out.println(
          "Warning: The RosettaLanguage \""
              + idLower
              + "\" is already registered and its adapted class will be overwritten. ("
              + LANGUAGES.get(idLower).getName()
              + " becomes "
              + type.getName()
              + ")");
    }
    LANGUAGES.put(id, type);
  }

  /**
   * @param id The YAML ID of the application.
   * @return An RosettaApplication instance.
   * @throws NullPointerException If the ID or the type are null.
   * @throws UnsupportedApplicationException If the application ID is unrecognized.
   */
  @NotNull
  public static RosettaApplication createApplication(@NotNull String id) {
    return createApplication(id, RosettaApplication.class);
  }

  /**
   * @param id The YAML ID of the language.
   * @return A language instance.
   * @throws NullPointerException If the ID or the type are null.
   * @throws UnsupportedLanguageException If the language ID is unrecognized.
   */
  @NotNull
  public static RosettaLanguage<?, ?> createLanguage(@NotNull String id) {
    return createLanguage(id, RosettaLanguage.class);
  }

  /**
   * @param id The YAML ID of the application.
   * @param type The class-type to cast. (Must implement RosettaApplication)
   * @param <E> The class-type to cast. (Must implement RosettaApplication)
   * @return A RosettaApplication instance.
   * @throws NullPointerException If the ID or the type are null.
   * @throws RuntimeException If any instantiation reflection API throws an exception.
   * @throws UnsupportedLanguageException If the application ID is unrecognized.
   */
  @NotNull
  @SuppressWarnings({"unchecked", "unused"})
  public static <E extends RosettaApplication> E createApplication(
      @NotNull String id, @NotNull Class<E> type) {

    String idLower = id.toLowerCase();

    Class<E> clazz = (Class<E>) APPLICATIONS.get(idLower);
    if (clazz == null) {
      throw new UnsupportedApplicationException(idLower);
    }

    try {
      Constructor<E> constructor = clazz.getConstructor();
      if (!constructor.canAccess(null)) {
        throw new RuntimeException(
            "The no-arg constructor is not publicly accessible: " + clazz.getName());
      }
      return constructor.newInstance();
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("No empty-constructor exists: " + clazz.getName(), e);
    } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Failed to instantiate: " + clazz.getName(), e);
    }
  }

  /**
   * @param id The YAML ID of the language.
   * @param type The class-type to cast. (Must implement RosettaLanguage)
   * @param <E> The class-type to cast. (Must implement RosettaLanguage)
   * @return An RosettaLanguage instance.
   * @throws NullPointerException If the ID or the type are null.
   * @throws RuntimeException If any instantiation reflection API throws an exception.
   * @throws UnsupportedApplicationException If the language ID is unrecognized.
   */
  @NotNull
  @SuppressWarnings({"unchecked", "unused"})
  public static <E extends RosettaLanguage<?, ?>> E createLanguage(
      @NotNull String id, @NotNull Class<E> type) {

    String idLower = id.toLowerCase();

    Class<E> clazz = (Class<E>) LANGUAGES.get(idLower);
    if (clazz == null) {
      throw new UnsupportedLanguageException(idLower);
    }

    try {
      Constructor<E> constructor = clazz.getConstructor();
      if (!constructor.canAccess(null)) {
        throw new RuntimeException(
            "The no-arg constructor is not publicly accessible: " + clazz.getName());
      }
      return constructor.newInstance();
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("No empty-constructor exists: " + clazz.getName(), e);
    } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Failed to instantiate: " + clazz.getName(), e);
    }
  }

  @NotNull
  public static RosettaCollection load(
      @NotNull TriConsumer<String, RosettaApplication, Map<String, Object>> applicationCallback,
      @NotNull TriConsumer<String, RosettaLanguage<?, ?>, Map<String, Object>> languageCallback,
      @NotNull File file)
      throws IOException {
    RosettaCollection collection = new RosettaCollection();
    collection.load(applicationCallback, languageCallback, file);
    return collection;
  }

  @NotNull
  public static RosettaCollection createCollection() {
    return new RosettaCollection();
  }

  @NotNull
  public static Dump getYamlWriter() {
    return DEFAULT_DUMP;
  }

  @NotNull
  public static Load getYamlReader() {
    return DEFAULT_LOAD;
  }
}
