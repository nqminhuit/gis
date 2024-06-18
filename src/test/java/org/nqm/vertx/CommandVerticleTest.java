package org.nqm.vertx;

// import static org.assertj.core.api.Assertions.assertThat;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.vertx.core.Vertx;

class CommandVerticleTest {

  private Vertx vertx;
  private final PrintStream out = System.out;
  private final ByteArrayOutputStream outCaptor = new ByteArrayOutputStream();

  @BeforeEach
  void setup() {
    vertx = GisVertx.instance();
    System.setOut(new PrintStream(outCaptor));
  }

  @AfterEach
  void teardown() {
    System.setOut(out);
  }

  @Test
  void commandVerticle_OK() throws Exception {
    // given:
    var path = Path.of("/", "home", "minh", "projects", "ocm-system");
    var verticle = new CommandVerticle(
        path, new String[] {"status", "-sb", "--ignore-submodules", "--porcelain=v2", "--gis-one-line"});

    // when:
    vertx.deployVerticle(verticle);
    // console output: ocm-system develop README.md cmonp.yaml pom.xml den.jv

    // then:
    // assertThat(outCaptor.toString().trim()).isEqualTo("");
  }
}
