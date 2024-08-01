package org.nqm.helper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.net.http.HttpResponse.ResponseInfo;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.nqm.config.GisConfig;
import org.testcontainers.containers.GenericContainer;

public abstract class GitBaseTest extends StdBaseTest {

  /**
   * To be able to run testcontainer with podman, execute 3 commands below:
   * <ul>
   * <li>systemctl --user enable podman.socket --now</li>
   * <li>export TESTCONTAINERS_RYUK_DISABLED=true</li>
   * <li>export DOCKER_HOST=unix:///run/user/${UID}/podman/podman.sock</li>
   * </ul>
   */
  protected static GenericContainer<?> container;

  protected static final HttpClient http = HttpClient.newHttpClient();

  protected static int gitbucketPort = 0;

  @TempDir
  protected Path tempPath;

  @BeforeAll
  static void beforeAll() {
    container = new GenericContainer<>("ghcr.io/gitbucket/gitbucket:4.39.0");
    container.withExposedPorts(8080);
    container.start();

    gitbucketPort = container.getMappedPort(8080);
  }

  @AfterAll
  static void afterAll() {
    container.stop();
  }

  protected void ignoreMarkerFile() throws IOException {
    var markerFile = tempPath.resolve(".gis-modules");
    if (!Files.exists(markerFile)) {
      Files.createFile(markerFile);
    }
    var gitIgnoreFile = tempPath.resolve(".gitignore");
    Files.createFile(gitIgnoreFile);
    Files.writeString(gitIgnoreFile, ".gis-modules");
  }

  public static void git(Path p, String... args) {
    var commands = Stream.concat(Stream.of(GisConfig.GIT_HOME_DIR), Stream.of(args))
        .toArray(String[]::new);
    int exitCode = 0;
    try {
      exitCode = new ProcessBuilder(commands)
          .directory(p.toFile())
          .start()
          .waitFor();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
      Thread.currentThread().interrupt();
    }
    if (exitCode != 0) {
      throw new RuntimeException("could not execute commands: '%s' because exit code = '%d'"
          .formatted(String.join(" ", commands), exitCode));
    }
  }

  protected List<Path> create_clone_gitRepositories(String... repos) {
    final var baseUrl = "http://root:root@localhost:%s/git/root".formatted(gitbucketPort);
    return Stream.of(repos)
        .map(repo -> {
          try {
            createRemoteRepo(repo);
            git(tempPath, "clone", baseUrl + "/%s.git".formatted(repo));
            return tempPath.resolve(repo);
          } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException("Could not prepare git repository");
          }
        })
        .toList();
  }

  protected void createRemoteRepo(String repoName)
      throws IOException, InterruptedException, URISyntaxException {
    var req = HttpRequest.newBuilder()
        .POST(BodyPublishers.ofString("{\"name\":\"%s\",\"private\":false}".formatted(repoName)))
        .uri(new URI("http://localhost:%s/api/v3/user/repos".formatted(gitbucketPort)))
        .header("accept", "application/json")
        .header("authorization", "Basic cm9vdDpyb290")
        .build();

    http.send(req, new BodyHandler<String>() {

      @Override
      public BodySubscriber<String> apply(ResponseInfo resp) {
        var statusCode = resp.statusCode();
        if (200 != statusCode) {
          throw new IllegalArgumentException("Could not execute http request");
        }
        return BodySubscribers.ofString(StandardCharsets.UTF_8);
      }
    });
  }

  private static void safelyCreateFile(Path file) {
    try {
      Files.createFile(file);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected void commitFile(List<Path> repos) {
    var filename = "" + UUID.randomUUID();
    repos.forEach(repo -> {
      var file = repo.resolve(filename);
      git(repo, "config", "user.email", "Your@Name.Com", "-v");
      git(repo, "config", "user.name", "Your Name", "-v");
      safelyCreateFile(file);
      git(repo, "add", "" + file, "-v");
      git(repo, "commit", "-m", "commit file", "-v");
    });
  }

  public static void scrambleFiles(List<Path> repos) {
    repos.forEach(repo -> {
      var file = repo.resolve("filescramble1");
      try {
        Files.createFile(file);
        git(repo, "add", "" + file);
        Files.write(file, "asdf".getBytes());
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  protected void resetHead1(List<Path> repos) {
    repos.forEach(repo -> {
      git(repo, "reset", "HEAD~1");
    });
  }

  protected void cleanUntrackedFiles(List<Path> repos) {
    repos.forEach(repo -> {
      git(repo, "clean", "-fd");
    });
  }

}
