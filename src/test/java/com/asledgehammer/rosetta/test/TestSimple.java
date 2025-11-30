package com.asledgehammer.rosetta.test;

import com.asledgehammer.rosetta.Rosetta;
import com.asledgehammer.rosetta.RosettaCollection;
import com.asledgehammer.rosetta.java.*;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class TestSimple {

  static class MyArrayList extends ArrayList<String> {}

  @Test
  public void test() {

    JavaDiscoverySettings settings = new JavaDiscoverySettings();
    settings.setSuperPolicy(JavaDiscoverySettings.SuperPolicy.EXPOSE);

    JavaLanguage language = new JavaLanguage();
    JavaClass javaClass = language.of(settings, MyArrayList.class);

    RosettaCollection collection = Rosetta.createCollection();
    collection.addLanguage(language);

    final JavaSerializeSettings javaSerializeSettings = new JavaSerializeSettings();

    String yaml =
        collection.save(
            "test",
            (id, app) -> Map.of(),
            (id, lang) -> {
              if (lang instanceof JavaLanguage javaLanguage) {
                return javaLanguage.onSave(javaSerializeSettings, id);
              }
              return Map.of();
            });

    System.out.println(yaml);

    //    System.out.println(collection.save(() => {
    //      serializeSettings
    //    }));
  }
}
