package org.nqm.utils;

import java.util.function.Supplier;

public class ExceptionUtils {

  private ExceptionUtils() {}

  // Reason to use Supplier over new instance: https://stackoverflow.com/a/47264712/12381095
  public static void throwIf(boolean b, Supplier<? extends RuntimeException> e) {
    if (b) {
      throw e.get();
    }
  }
}
