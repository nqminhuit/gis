package org.nqm.helper;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class StdBaseTest {

  protected final PrintStream out = System.out;
  protected final PrintStream err = System.err;
  protected final ByteArrayOutputStream outCaptor = new ByteArrayOutputStream();
  protected final ByteArrayOutputStream errCaptor = new ByteArrayOutputStream();

  @BeforeEach
  protected void setup() {
    System.setOut(new PrintStream(outCaptor));
    System.setErr(new PrintStream(errCaptor));
  }

  @AfterEach
  protected void teardown() {
    System.setOut(out);
    System.setErr(err);
  }

}
