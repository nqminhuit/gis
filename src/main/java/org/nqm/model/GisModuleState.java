package org.nqm.model;

/**
 * State of a module while the current command is running.
 */
public enum GisModuleState {

  PENDING("pending"),
  IN_PROGRESS("in-progress"),
  DONE("done"),
  FAILED("failed");

  private final String value;

  GisModuleState(String value) {
    this.value = value;
  }

  /**
   * The value reported to clients, which may hold characters an enum name cannot.
   */
  public String value() {
    return value;
  }
}
