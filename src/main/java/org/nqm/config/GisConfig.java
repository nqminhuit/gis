package org.nqm.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import org.nqm.GisException;
import org.nqm.utils.StdOutUtils;

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

  private static final String DONT_CARE_FILES_KEY = "dont_care_files";
  private static final String[] DONT_CARE_FILES_VALS = new String[] {};

  private static final String CURRENT_DIR = "" + Path.of("").toAbsolutePath();
  public static final String GIT_HOME_DIR = "/usr/bin/git";

  private static final String MODULE_TIMEOUT_KEY = "module_timeout_seconds";
  private static final int MODULE_TIMEOUT_DEFAULT = 60;

  public static int getModuleTimeoutSeconds() {
    return parseModuleTimeoutSeconds(props.getProperty(MODULE_TIMEOUT_KEY));
  }

  static int parseModuleTimeoutSeconds(String val) {
    if (val == null || val.isBlank()) {
      return MODULE_TIMEOUT_DEFAULT;
    }
    int parsed;
    try {
      parsed = Integer.parseInt(val.trim());
    } catch (NumberFormatException e) {
      StdOutUtils.warnln("config '%s=%s' is not a number, falling back to %ds"
          .formatted(MODULE_TIMEOUT_KEY, val, MODULE_TIMEOUT_DEFAULT));
      return MODULE_TIMEOUT_DEFAULT;
    }
    if (parsed <= 0) {
      StdOutUtils.warnln("config '%s=%s' must be positive, falling back to %ds"
          .formatted(MODULE_TIMEOUT_KEY, val, MODULE_TIMEOUT_DEFAULT));
      return MODULE_TIMEOUT_DEFAULT;
    }
    return parsed;
  }

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
        .map(splitValue)
        .orElse(FEATURE_BRANCH_PREFIX_VALS);
  }

  public static String[] getDontCareFiles() {
    return Optional.of(props)
        .map(props -> props.getProperty(DONT_CARE_FILES_KEY))
        .map(splitValue)
        .orElse(DONT_CARE_FILES_VALS);
  }

  public static String currentDir() {
    return CURRENT_DIR;
  }
}
