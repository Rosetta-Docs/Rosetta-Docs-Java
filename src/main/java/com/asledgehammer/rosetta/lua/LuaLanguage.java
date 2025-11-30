package com.asledgehammer.rosetta.lua;

import com.asledgehammer.rosetta.RosettaLanguage;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class LuaLanguage implements RosettaLanguage<LuaSerializeSettings, LuaDeserializeSettings> {

  @Override
  public void onLoad(
      @NotNull LuaDeserializeSettings settings,
      @NotNull String id,
      @NotNull Map<String, Object> language) {}

  @NotNull
  @Override
  public Map<String, Object> onSave(@NotNull LuaSerializeSettings settings, @NotNull String id) {
    return Map.of();
  }

  @NotNull
  @Override
  public String getID() {
    return "lua";
  }
}
