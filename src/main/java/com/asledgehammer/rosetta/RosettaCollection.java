package com.asledgehammer.rosetta;

import com.asledgehammer.rosetta.exception.RosettaException;
import java.io.*;
import java.util.*;
import java.util.function.BiFunction;

import com.asledgehammer.rosetta.exception.ValueTypeException;
import org.jetbrains.annotations.NotNull;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.Load;

/**
 * RosettaCollection store a group or collection of Rosetta data.
 *
 * <p>Rosetta has two main dataset schema types:
 *
 * <ul>
 *   <li>Language-specific: Language-generic datasets.
 *   <li>Application-specific: Application-specific datasets of language data structured in a way
 *       that applies to said-application.
 * </ul>
 */
public class RosettaCollection {

  private final Map<String, RosettaLanguage<?, ?>> languages = new HashMap<>();
  private final Map<String, RosettaApplication> applications = new HashMap<>();

  private String locale = "EN_US";
  private final String version = "1.2";

  public RosettaCollection() {}

  /**
   * Loads rosetta data from a {@link File}.
   *
   * @param file The file storing the YAML encoded data.
   * @throws NullPointerException If the file is null.
   * @throws IOException If something is thrown during the process of loading and reading the file.
   */
  @SuppressWarnings({"unchecked"})
  public void load(
      @NotNull TriConsumer<String, RosettaApplication, Map<String, Object>> applicationCallback,
      @NotNull TriConsumer<String, RosettaLanguage<?, ?>, Map<String, Object>> languageCallback,
      @NotNull File file)
      throws IOException {
    Load reader = Rosetta.getYamlReader();
    Object raw = reader.loadAllFromReader(new FileReader(file));
    if (!(raw instanceof Map)) {
      throw new RosettaException(
          "Invalid YAML root type: " + raw.getClass().getName() + " (Must be dictionary/Map)");
    }
    onLoad(applicationCallback, languageCallback, (Map<String, Object>) raw);
  }

  /**
   * Loads rosetta data from an {@link Reader}.
   *
   * @param reader The reader transmitting the YAML encoded data.
   * @throws NullPointerException If the reader is null.
   */
  @SuppressWarnings({"unchecked"})
  public void load(
      @NotNull TriConsumer<String, RosettaApplication, Map<String, Object>> applicationCallback,
      @NotNull TriConsumer<String, RosettaLanguage<?, ?>, Map<String, Object>> languageCallback,
      @NotNull Reader reader) {
    Load load = Rosetta.getYamlReader();
    Object raw = load.loadAllFromReader(reader);
    if (!(raw instanceof Map)) {
      throw new RosettaException(
          "Invalid YAML root type: " + raw.getClass().getName() + " (Must be dictionary/Map)");
    }
    onLoad(applicationCallback, languageCallback, (Map<String, Object>) raw);
  }

  /**
   * Loads rosetta data from an {@link InputStream}.
   *
   * @param stream The stream transmitting the YAML encoded data.
   * @throws NullPointerException If the stream is null.
   */
  @SuppressWarnings({"unchecked"})
  public void load(
      @NotNull TriConsumer<String, RosettaApplication, Map<String, Object>> applicationCallback,
      @NotNull TriConsumer<String, RosettaLanguage<?, ?>, Map<String, Object>> languageCallback,
      @NotNull InputStream stream) {
    Load load = Rosetta.getYamlReader();
    Object raw = load.loadFromInputStream(stream);
    if (!(raw instanceof Map)) {
      throw new RosettaException(
          "Invalid YAML root type: " + raw.getClass().getName() + " (Must be dictionary/Map)");
    }
    onLoad(applicationCallback, languageCallback, (Map<String, Object>) raw);
  }

  /**
   * Loads rosetta data from a YAML string.
   *
   * @param yaml The YAML encoded data.
   * @throws NullPointerException If the YAML string is null.
   */
  @SuppressWarnings({"unchecked"})
  public void load(
      @NotNull TriConsumer<String, RosettaApplication, Map<String, Object>> applicationCallback,
      @NotNull TriConsumer<String, RosettaLanguage<?, ?>, Map<String, Object>> languageCallback,
      @NotNull String yaml) {

    if (yaml.isEmpty()) {
      throw new IllegalArgumentException("The YAML string is empty.");
    }

    Load reader = Rosetta.getYamlReader();
    Iterable<Object> oRaw = reader.loadAllFromString(yaml);

    if (!(oRaw instanceof Map)) {
      throw new RuntimeException("Improperly formatted Rosetta YAML:\n" + yaml);
    }

    onLoad(applicationCallback, languageCallback, (Map<String, Object>) oRaw);
  }

  /**
   * @param data The serialized map of rosetta data to process.
   * @throws NullPointerException If the raw map is null.
   */
  @SuppressWarnings({"unchecked"})
  private void onLoad(
      @NotNull TriConsumer<String, RosettaApplication, Map<String, Object>> applicationCallback,
      @NotNull TriConsumer<String, RosettaLanguage<?, ?>, Map<String, Object>> languageCallback,
      @NotNull Map<String, Object> data) {

    if (!data.containsKey("id")) {
      throw new RosettaException("Missing \"id\" property at root of Rosetta YAML file.");
    }

    final String id = data.get("id").toString().trim();

    // Grab the key.
    if (!data.containsKey("version")) {
      throw new RosettaException("Missing \"version\" property at root of Rosetta YAML file.");
    }

    // If multi-version support in the future, convert to switch-table.
    final String version = data.get("version").toString().trim();
    if (!version.equals("1.2")) {
      throw new RosettaException("Unknown version: " + version);
    }

    // If a locale is defined, apply it.
    if (data.containsKey("locale")) {
      Object oLocale = data.get("locale");
      if (!(oLocale instanceof String)) {
        throw new ValueTypeException("[ROOT]", "locale", oLocale.getClass(), String.class);
      }
      this.locale = (String) oLocale;
    } else {
      // (Default is `EN_US`)
      this.locale = "EN_US";
    }

    if (data.containsKey("languages")) {
      final Object oLanguages = data.get("languages");
      if (!(oLanguages instanceof Map)) {
        throw new ValueTypeException("<ROOT>", "languages", oLanguages.getClass(), Map.class);
      }
      onLoadLanguages(id, languageCallback, (Map<String, Object>) oLanguages);
    }

    if (data.containsKey("applications")) {
      final Object oApplications = data.get("applications");
      if (!(oApplications instanceof Map)) {
        throw new ValueTypeException("<ROOT>", "applications", oApplications.getClass(), Map.class);
      }
      onLoadApplications(id, applicationCallback, (Map<String, Object>) oApplications);
    }
  }

  /**
   * @param languages The serialized map of languages data to process.
   * @throws NullPointerException If the languages map is null.
   */
  @SuppressWarnings({"unchecked"})
  private void onLoadLanguages(
      @NotNull String id,
      @NotNull TriConsumer<String, RosettaLanguage<?, ?>, Map<String, Object>> languageCallback,
      @NotNull Map<String, Object> languages) {
    final List<String> keys = new ArrayList<>(languages.keySet());
    keys.sort(Comparator.naturalOrder());

    for (String key : keys) {
      String keyLower = key.toLowerCase().trim();
      RosettaLanguage<?, ?> language;
      if (!hasLanguage(keyLower)) {
        language = Rosetta.createLanguage(keyLower);
        this.languages.put(keyLower, language);
      } else {
        language = this.languages.get(keyLower);
      }
      /* language.onLoad((Map<String, Object>) languages.get(key)); */
      languageCallback.accept(id, language, (Map<String, Object>) languages.get(key));
    }
  }

  /**
   * @param applications The serialized map of application data to process.
   * @throws NullPointerException If the applications map is null.
   */
  @SuppressWarnings({"unchecked"})
  private void onLoadApplications(
      @NotNull String id,
      @NotNull TriConsumer<String, RosettaApplication, Map<String, Object>> applicationCallback,
      @NotNull Map<String, Object> applications) {
    final List<String> keys = new ArrayList<>(applications.keySet());
    keys.sort(Comparator.naturalOrder());

    for (String key : keys) {
      String keyLower = key.toLowerCase().trim();
      RosettaApplication application;
      if (!hasLanguage(keyLower)) {
        application = Rosetta.createApplication(keyLower);
        this.applications.put(keyLower, application);
      } else {
        application = this.applications.get(keyLower);
      }
      /* application.onLoad((Map<String, Object>) applications.get(key)); */
      applicationCallback.accept(id, application, (Map<String, Object>) applications.get(key));
    }
  }

  /**
   * @param file The file to write.
   * @throws NullPointerException If the file is null.
   */
  public void save(
      @NotNull String id,
      @NotNull BiFunction<String, RosettaApplication, Map<String, Object>> applicationCallback,
      @NotNull BiFunction<String, RosettaLanguage<?, ?>, Map<String, Object>> languageCallback,
      @NotNull File file) {
    try (FileWriter fw = new FileWriter(file)) {
      fw.write(save(id, applicationCallback, languageCallback));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param writer The writer to write.
   * @throws NullPointerException If the writer is null.
   * @throws IOException If something happens during the writing of contents.
   */
  public void save(
      @NotNull String id,
      @NotNull BiFunction<String, RosettaApplication, Map<String, Object>> applicationCallback,
      @NotNull BiFunction<String, RosettaLanguage<?, ?>, Map<String, Object>> languageCallback,
      @NotNull BufferedWriter writer)
      throws IOException {
    writer.write(save(id, applicationCallback, languageCallback));
  }

  /**
   * @param stream The stream to write.
   * @throws NullPointerException If the stream is null.
   * @throws IOException If something happens during the writing of contents.
   */
  public void save(
      @NotNull String id,
      @NotNull BiFunction<String, RosettaApplication, Map<String, Object>> applicationCallback,
      @NotNull BiFunction<String, RosettaLanguage<?, ?>, Map<String, Object>> languageCallback,
      @NotNull DataOutputStream stream)
      throws IOException {
    stream.writeUTF(save(id, applicationCallback, languageCallback));
  }

  /**
   * @return A YAML-Serialized string.
   */
  @NotNull
  public String save(
      @NotNull String id,
      @NotNull BiFunction<String, RosettaApplication, Map<String, Object>> applicationCallback,
      @NotNull BiFunction<String, RosettaLanguage<?, ?>, Map<String, Object>> languageCallback) {
    return save(id, applicationCallback, languageCallback, Rosetta.getYamlWriter());
  }

  /**
   * @param writer The SnakeYAML Dump instance to serialize the collection.
   * @return A YAML-Serialized string.
   * @throws NullPointerException If the writer is null.
   */
  @NotNull
  public String save(
      @NotNull String id,
      @NotNull BiFunction<String, RosettaApplication, Map<String, Object>> applicationCallback,
      @NotNull BiFunction<String, RosettaLanguage<?, ?>, Map<String, Object>> languageCallback,
      @NotNull Dump writer) {
    return writer.dumpToString(onSave(id, applicationCallback, languageCallback));
  }

  @NotNull
  public Map<String, Object> onSave(
      @NotNull String id,
      @NotNull BiFunction<String, RosettaApplication, Map<String, Object>> applicationCallback,
      @NotNull BiFunction<String, RosettaLanguage<?, ?>, Map<String, Object>> languageCallback) {

    final Map<String, Object> raw = new HashMap<>();

    raw.put("id", id);
    raw.put("version", version);
    raw.put("locale", locale);

    if (hasLanguages()) {
      final Map<String, Object> languages = new HashMap<>();
      List<String> keys = new ArrayList<>(this.languages.keySet());
      keys.sort(Comparator.naturalOrder());
      for (String key : keys) {
        languages.put(
            key,
            /* this.languages.get(key).onSave() */
            languageCallback.apply(id, this.languages.get(key)));
      }
      raw.put("languages", languages);
    }

    if (hasApplications()) {
      final Map<String, Object> applications = new HashMap<>();
      List<String> keys = new ArrayList<>(this.applications.keySet());
      keys.sort(Comparator.naturalOrder());
      for (String key : keys) {
        applications.put(
            key,
            /* this.applications.get(key).onSave() */
            applicationCallback.apply(id, this.applications.get(key)));
      }
      raw.put("applications", applications);
    }

    return raw;
  }

  private boolean hasLanguages() {
    return !this.languages.isEmpty();
  }

  private boolean hasApplications() {
    return !this.applications.isEmpty();
  }

  /**
   * Adds a RosettaLanguage to the RosettaCollection.
   *
   * @param lang The RosettaLanguage to register.
   * @throws IllegalArgumentException If the RosettaLanguage's ID marches an existing
   *     RosettaLanguage object that's already in the collection.
   */
  public void addLanguage(@NotNull RosettaLanguage<?, ?> lang) {
    String idLower = lang.getID().toLowerCase();
    if (languages.containsKey(idLower)) {
      throw new IllegalArgumentException(
          "A RosettaLanguage already exists with the ID: " + idLower);
    }

    languages.put(idLower, lang);
  }

  /**
   * Removes a RosettaLanguage from the RosettaCollection.
   *
   * @param lang The RosettaLanguage to unregister.
   * @throws IllegalArgumentException If the RosettaLanguage's ID doesn't march an existing
   *     RosettaLanguage object that's in the collection.
   */
  public void removeLanguage(@NotNull RosettaLanguage<?, ?> lang) {

    String idLower = lang.getID().toLowerCase();
    if (!languages.containsKey(idLower)) {
      throw new NullPointerException("No RosettaLanguage is registered with the ID: " + idLower);
    }

    languages.remove(lang.getID());
  }

  /**
   * Removes a RosettaLanguage from the RosettaCollection.
   *
   * @param id The YAML ID of the RosettaLanguage to unregister.
   * @return The unregistered RosettaLanguage.
   * @throws IllegalArgumentException If the RosettaLanguage's ID doesn't march an existing
   *     RosettaLanguage object that's in the collection.
   */
  @NotNull
  public RosettaLanguage<?, ?> removeLanguage(@NotNull String id) {

    String idLower = id.toLowerCase();
    if (!languages.containsKey(idLower)) {
      throw new NullPointerException("No RosettaLanguage is registered with the ID: " + idLower);
    }

    return languages.remove(id.toLowerCase());
  }

  /**
   * @param id The YAML language ID.
   * @return The registered RosettaLanguage object.
   * @throws NullPointerException If no RosettaLanguage object is registered with the ID. Use {@link
   *     RosettaCollection#hasLanguage(String)} to see if a RosettaLanguage is registered with the
   *     ID before invoking this method.
   */
  public RosettaLanguage<?, ?> getLanguage(@NotNull String id) {

    String idLower = id.toLowerCase();
    if (!languages.containsKey(idLower)) {
      throw new NullPointerException("No RosettaLanguage is registered with the ID: " + idLower);
    }

    return languages.get(id);
  }

  /**
   * @param lang The RosettaLanguage to test.
   * @return True if the RosettaLanguage is registered.
   */
  public boolean hasLanguage(@NotNull RosettaLanguage<?, ?> lang) {
    return hasLanguage(lang.getID());
  }

  /**
   * @param id The RosettaLanguage ID to test.
   * @return True if a RosettaLanguage with the ID is registered.
   */
  public boolean hasLanguage(@NotNull String id) {
    return languages.containsKey(id.toLowerCase());
  }

  /**
   * Adds a RosettaApplication to the collection.
   *
   * @param app The app to register.
   * @throws IllegalArgumentException If the RosettaApplication's ID marches an existing
   *     RosettaApplication object that's already in the collection.
   */
  public void addApplication(@NotNull RosettaApplication app) {
    String idLower = app.getID().toLowerCase();
    if (applications.containsKey(idLower)) {
      throw new IllegalArgumentException(
          "A RosettaApplication already exists with the ID: " + idLower);
    }

    applications.put(idLower, app);
  }

  /**
   * Removes a RosettaApplication from the collection.
   *
   * @param app The RosettaApplication to unregister.
   * @throws IllegalArgumentException If the RosettaApplication's ID doesn't march an existing
   *     RosettaApplication object that's in the collection.
   */
  public void removeApplication(@NotNull RosettaApplication app) {

    String idLower = app.getID().toLowerCase();
    if (!applications.containsKey(idLower)) {
      throw new NullPointerException("No RosettaApplication is registered with the ID: " + idLower);
    }

    applications.remove(app.getID());
  }

  /**
   * Removes a RosettaApplication from the collection.
   *
   * @param id The YAML ID of the RosettaApplication to unregister.
   * @return The unregistered RosettaApplication.
   * @throws IllegalArgumentException If the RosettaApplication's ID doesn't march an existing
   *     RosettaApplication object that's in the collection.
   */
  @NotNull
  public RosettaApplication removeApplication(@NotNull String id) {

    String idLower = id.toLowerCase();
    if (!applications.containsKey(idLower)) {
      throw new NullPointerException("No RosettaApplication is registered with the ID: " + idLower);
    }

    return applications.remove(id.toLowerCase());
  }

  /**
   * @param id The YAML language ID.
   * @return The registered RosettaApplication object.
   * @throws NullPointerException If no RosettaApplication object is registered with the ID. Use
   *     {@link RosettaCollection#hasLanguage(String)} to see if a RosettaApplication is registered
   *     with the ID before invoking this method.
   */
  public RosettaApplication getApplication(@NotNull String id) {

    String idLower = id.toLowerCase();
    if (!applications.containsKey(idLower)) {
      throw new NullPointerException("No RosettaApplication is registered with the ID: " + idLower);
    }

    return applications.get(id);
  }

  /**
   * @param app The RosettaApplication to test.
   * @return True if the RosettaApplication is registered in the app.
   */
  public boolean hasApplication(@NotNull RosettaApplication app) {
    return hasApplication(app.getID());
  }

  /**
   * @param id The RosettaApplication ID to test.
   * @return True if a RosettaApplication with the ID is registered in the collection.
   */
  public boolean hasApplication(@NotNull String id) {
    return applications.containsKey(id.toLowerCase());
  }

  @NotNull
  public String getVersion() {
    return this.version;
  }

  @NotNull
  public String getLocale() {
    return this.locale;
  }

  public void setLocale(@NotNull String locale) {
    this.locale = locale;
  }
}
