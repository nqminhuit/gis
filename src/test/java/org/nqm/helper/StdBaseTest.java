package org.nqm.helper;

import static org.nqm.utils.StdOutUtils.CL_BLACK;
import static org.nqm.utils.StdOutUtils.CL_BLUE;
import static org.nqm.utils.StdOutUtils.CL_CYAN;
import static org.nqm.utils.StdOutUtils.CL_GREEN;
import static org.nqm.utils.StdOutUtils.CL_PURPLE;
import static org.nqm.utils.StdOutUtils.CL_RED;
import static org.nqm.utils.StdOutUtils.CL_RESET;
import static org.nqm.utils.StdOutUtils.CL_WHITE;
import static org.nqm.utils.StdOutUtils.CL_YELLOW;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.nqm.config.GisLog;
import org.nqm.utils.StdOutUtils;

public abstract class StdBaseTest {

  protected final PrintStream out = System.out;
  protected final PrintStream err = System.err;
  protected final InputStream in = System.in;
  protected ByteArrayOutputStream outCaptor = new ByteArrayOutputStream();
  protected ByteArrayOutputStream errCaptor = new ByteArrayOutputStream();

  protected void additionalSetup() throws IOException {}

  protected void additionalTeardown() throws IOException {}

  @BeforeEach
  protected void setup() throws IOException {
    System.setOut(new PrintStream(outCaptor));
    System.setErr(new PrintStream(errCaptor));
    additionalSetup();
  }

  @AfterEach
  protected void teardown() throws IOException {
    System.setOut(out);
    System.setErr(err);
    System.setIn(in);
    GisLog.setIsDebugEnabled(false);
    StdOutUtils.setMuteOutput(false);
    additionalTeardown();
  }

  protected void resetOutputStreamTest() {
    outCaptor = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outCaptor));
  }

  protected static Function<String, List<String>> stripColors =
      str -> Stream.of(str.split("%n".formatted()))
          .map(s -> s.replace(CL_RESET, ""))
          .map(s -> s.replace(CL_BLACK, ""))
          .map(s -> s.replace(CL_RED, ""))
          .map(s -> s.replace(CL_GREEN, ""))
          .map(s -> s.replace(CL_YELLOW, ""))
          .map(s -> s.replace(CL_BLUE, ""))
          .map(s -> s.replace(CL_PURPLE, ""))
          .map(s -> s.replace(CL_CYAN, ""))
          .map(s -> s.replace(CL_WHITE, ""))
          .toList();

  protected static Function<String, String> stripColorsToString =
      str -> str.replace(CL_RESET, "")
          .replace(CL_BLACK, "")
          .replace(CL_RED, "")
          .replace(CL_GREEN, "")
          .replace(CL_YELLOW, "")
          .replace(CL_BLUE, "")
          .replace(CL_PURPLE, "")
          .replace(CL_CYAN, "")
          .replace(CL_WHITE, "");

}
