package org.nqm.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import org.nqm.exception.GisException;

public class GisConfig {

  private GisConfig() {}

  private static final Properties props;

  static {
    var path = Path.of(System.getProperty("user.home"), ".config", "gis.config");
    props = new Properties();
    if (path.toFile().exists()) {
      try (var stream = Files.newInputStream(path)) {
        props.load(stream);
      } catch (IOException e) {
        throw new GisException("Could not load user config because: " + e.getMessage());
      }
    }
  }

  private static final String DEFAULT_BRANCHES_KEY = "default_branches";
  private static final String[] DEFAULT_BRANCH_VALS = new String[] {"master", "main", "develop"};

  private static final String FEATURE_BRANCH_PREFIXES_KEY = "feature_branch_prefixes";
  private static final String[] FEATURE_BRANCH_PREFIX_VALS = new String[] {"feature/"};

  public static final String CURRENT_DIR = "" + Path.of("").toAbsolutePath();
  public static final String GIT_HOME_DIR = "/usr/bin/git";

  private static Function<String, String[]> splitValue = val -> val.split(",");

  public static String[] getDefaultBranches() {
    return Optional.of(props)
        .map(props -> props.getProperty(DEFAULT_BRANCHES_KEY))
        .map(splitValue)
        .orElse(DEFAULT_BRANCH_VALS);
  }

  public static String[] getFeatureBranchPrefixes() {
    return Optional.of(props)
        .map(props -> props.getProperty(FEATURE_BRANCH_PREFIXES_KEY))
        .map(String::toLowerCase)
        .map(splitValue)
        .orElse(FEATURE_BRANCH_PREFIX_VALS);
  }
}
