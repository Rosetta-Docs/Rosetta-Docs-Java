package com.asledgehammer.rosetta;

import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * RosettaApplication handles the loading, storing, and serialization of application-level
 * definitions in Rosetta.
 *
 * <p>Implementations <b>must</b> have a public constructor with no arguments to work. Register them
 * using {@link Rosetta#registerApplication(String, Class)}.
 */
public interface RosettaApplication {

  /**
   * @param application The YAML dictionary storing the application data.
   */
  void onLoad(@NotNull Map<String, Object> application);

  /**
   * @return The serialized dictionary of all Rosetta entries for the application.
   */
  @NotNull
  Map<String, Object> onSave();

  /**
   * @return The YAML application name. E.G: `projectzomboid`, `myapp`, etc..
   */
  @NotNull
  String getID();
}
