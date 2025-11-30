package com.asledgehammer.rosetta.test;

import com.asledgehammer.rosetta.java.reference.SimpleTypeReference;
import com.asledgehammer.rosetta.java.reference.TypeReference;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

public class TestTypeReferenceType {

  ArrayList<String> list;

  @Test
  public void test() {
    Type type = null;
    try {
      Field field = getClass().getDeclaredField("list");
      type = field.getGenericType();
    } catch (Exception e) {
      e.printStackTrace();
    }
    TypeReference typeReference = TypeReference.of(type);
    assert typeReference.getBase().equals("java.util.ArrayList");
    assert typeReference instanceof SimpleTypeReference;
    assert ((SimpleTypeReference) typeReference).hasSubTypes();
    assert ((SimpleTypeReference) typeReference).getSubTypes().size() == 1;
    assert ((SimpleTypeReference) typeReference)
        .getSubTypes()
        .get(0)
        .getBase()
        .equals("java.lang.String");

    System.out.println(typeReference.getBase());
    System.out.println(((SimpleTypeReference) typeReference).getSubTypes());
  }
}
