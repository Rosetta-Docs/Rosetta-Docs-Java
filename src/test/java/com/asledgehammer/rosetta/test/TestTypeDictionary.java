package com.asledgehammer.rosetta.test;

import com.asledgehammer.rosetta.Rosetta;
import com.asledgehammer.rosetta.RosettaCollection;
import com.asledgehammer.rosetta.java.*;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class TestTypeDictionary {

  @Test
  public void test() {

    JavaDiscoverySettings settings = new JavaDiscoverySettings();
    settings.setSuperPolicy(JavaDiscoverySettings.SuperPolicy.IGNORE);

    JavaLanguage language = new JavaLanguage();
    language.of(settings, ArrayList.class);

    RosettaCollection collection = Rosetta.createCollection();
    collection.addLanguage(language);

    final JavaSerializeSettings javaSerializeSettings = new JavaSerializeSettings();
    javaSerializeSettings.setRenderTypesAsDictionary(true);

    String yaml =
        collection.save(
            "test", // For type-dictionary
            (id, app) -> Map.of(),
            (id, lang) -> {
              if (lang instanceof JavaLanguage javaLanguage) {
                return javaLanguage.onSave(javaSerializeSettings, id);
              }
              return Map.of();
            });

    System.out.println(yaml);
  }
}
