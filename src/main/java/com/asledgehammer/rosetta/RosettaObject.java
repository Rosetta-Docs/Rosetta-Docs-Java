package com.asledgehammer.rosetta;

/**
 * RosettaObject is a common super-class for dictionary objects that monitors its dirty-state for
 * compiling and modifying its properties.
 */
public abstract class RosettaObject implements DirtySupported {

  private boolean dirty = false;

  /** Generic creation constructor. No arguments are passed. */
  protected RosettaObject() {}

  @Override
  public boolean onCompile() {
    return true;
  }

  @Override
  public boolean isDirty() {
    return dirty;
  }

  @Override
  public void setDirty() {
    this.dirty = true;
  }

  public void setDirty(boolean flag) {
    this.dirty = flag;
  }
}
