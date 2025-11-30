package com.asledgehammer.rosetta;

import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * RosettaLanguage handles the loading, storing, and serialization of language-level definitions in
 * Rosetta.
 *
 * <p>Implementations <b>must</b> have a public constructor with no arguments to work. Register them
 * using {@link Rosetta#registerLanguage(String, Class)}.
 */
public interface RosettaLanguage<
    SerializeSettings extends RosettaSerializeSettings,
    DeserializeSettings extends RosettaDeserializeSettings> {

  /**
   * @param language The YAML dictionary storing the language data.
   */
  void onLoad(
      @NotNull DeserializeSettings settings,
      @NotNull String id,
      @NotNull Map<String, Object> language);

  /**
   * @return The serialized dictionary of all Rosetta entries for the language.
   */
  @NotNull
  Map<String, Object> onSave(@NotNull SerializeSettings settings, @NotNull String id);

  /**
   * @return The YAML language name. E.G: `java`, `lua`, etc..
   */
  @NotNull
  String getID();
}
