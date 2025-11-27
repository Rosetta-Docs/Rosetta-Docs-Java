package com.asledgehammer.rosetta.test;

import com.asledgehammer.rosetta.Rosetta;
import com.asledgehammer.rosetta.RosettaCollection;
import com.asledgehammer.rosetta.java.JavaExposureSettings;
import com.asledgehammer.rosetta.java.JavaClass;
import com.asledgehammer.rosetta.java.JavaLanguage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

public class TestSimple {

  static class MyArrayList extends ArrayList<String> {}

  @Test
  public void test() {

    JavaExposureSettings settings = new JavaExposureSettings();
    settings.setSuperPolicy(JavaExposureSettings.SuperPolicy.EXPOSE);

    JavaLanguage language = new JavaLanguage();
    JavaClass javaClass = language.of(MyArrayList.class, settings);

    System.out.println(javaClass);

    RosettaCollection collection = Rosetta.createCollection();
    collection.addLanguage(language);

    System.out.println(collection.save());
  }
}
