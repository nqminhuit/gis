package org.nqm.enums;

public enum GisAction {

  co("checkout"), st("status"), fe("fetch"), pu("pull"), push("push");

  private String fullAction;

  GisAction(String fullAction) {
    this.fullAction = fullAction;
  }

  // public static GisAction map(String action) {
  //     return Stream.of(GisAction.values())
  //         .filter(e -> e.getFullAction().equals(action))
  //         .findFirst()
  //         .orElse(ST);
  // }

  public String getFullAction() {
    return fullAction;
  }

}
