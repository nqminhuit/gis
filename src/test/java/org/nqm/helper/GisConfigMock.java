package org.nqm.helper;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.nqm.config.GisConfig;

public class GisConfigMock {

  private static MockedStatic<GisConfig> mock;

  public static void mockCurrentDirectory(String path) {
    if (mock == null || mock.isClosed()) {
      mock = Mockito.mockStatic(GisConfig.class);
    }
    mock.when(GisConfig::currentDir).thenReturn(path);
  }

  public static void mockBranchesColorDefault() {
    if (mock == null || mock.isClosed()) {
      mock = Mockito.mockStatic(GisConfig.class);
    }
    mock.when(GisConfig::getDefaultBranches).thenReturn(new String[] {"master", "main", "develop"});
    mock.when(GisConfig::getFeatureBranchPrefixes).thenReturn(new String[] {"feature/"});
  }

  public static void mockBranchesColorDefault(String[] defaultBranches, String[] prefixes) {
    if (mock == null || mock.isClosed()) {
      mock = Mockito.mockStatic(GisConfig.class);
    }
    mock.when(GisConfig::getDefaultBranches).thenReturn(defaultBranches);
    mock.when(GisConfig::getFeatureBranchPrefixes).thenReturn(prefixes);
  }

  public static void close() {
    if (mock != null && !mock.isClosed()) {
      mock.close();
    }
  }
}
