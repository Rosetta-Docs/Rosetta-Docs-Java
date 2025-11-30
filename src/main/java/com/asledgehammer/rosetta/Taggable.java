package com.asledgehammer.rosetta;

import com.asledgehammer.rosetta.java.JavaField;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface Taggable {

  @NotNull
  List<String> getTags();

  /**
   * Applies a tag to the object.
   *
   * @param tag The tag to apply.
   * @throws NullPointerException If the tag is null.
   * @throws IllegalArgumentException If the tag is empty or already applied.
   */
  void addTag(@NotNull String tag);

  default void addAllTags(@NotNull List<String> tags) {
    for (String tag : tags) {
      addTag(tag);
    }
  }

  /**
   * Removes a tag from the object.
   *
   * @param tag The tag to remove.
   * @throws NullPointerException If the tag is null.
   * @throws IllegalArgumentException If the tag is empty or is not applied.
   */
  void removeTag(@NotNull String tag);

  default void removeAllTags(@NotNull List<String> tags) {
    for (String tag : tags) {
      removeTag(tag);
    }
  }

  /**
   * Clears all applied tags.
   *
   * @return A list of the tags removed.
   * @throws RuntimeException If the object has no tags. (Use {@link JavaField#hasTags()})
   */
  @NotNull
  List<String> clearTags();

  /**
   * @return True if one or more tags are applied.
   */
  boolean hasTags();

  /**
   * @param tag The tag to evaluate.
   * @return True if the tag is registered.
   * @throws NullPointerException If the tag is null.
   * @throws IllegalArgumentException If the tag is empty.
   */
  boolean hasTag(@NotNull String tag);
}
