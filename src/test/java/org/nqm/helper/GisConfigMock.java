package org.nqm.helper;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.nqm.config.GisConfig;

public class GisConfigMock {

  private static MockedStatic<GisConfig> mock;

  private static void ensureMock() {
    if (mock == null || mock.isClosed()) {
      mock = Mockito.mockStatic(GisConfig.class);
      mock.when(GisConfig::getModuleTimeoutSeconds).thenReturn(60);
    }
  }

  public static void mockCurrentDirectory(String path) {
    ensureMock();
    mock.when(GisConfig::currentDir).thenReturn(path);
    mock.when(GisConfig::getDontCareFiles).thenReturn(new String[] {});
  }

  public static void mockBranchesColorDefault() {
    ensureMock();
    mock.when(GisConfig::getDefaultBranches).thenReturn(new String[] {"master", "main", "develop"});
    mock.when(GisConfig::getFeatureBranchPrefixes).thenReturn(new String[] {"feature/"});
    mock.when(GisConfig::getDontCareFiles).thenReturn(new String[] {});
  }

  public static void mockBranchesColorDefault(String[] defaultBranches, String[] prefixes) {
    ensureMock();
    mock.when(GisConfig::getDefaultBranches).thenReturn(defaultBranches);
    mock.when(GisConfig::getFeatureBranchPrefixes).thenReturn(prefixes);
    mock.when(GisConfig::getDontCareFiles).thenReturn(new String[] {});
  }

  public static void mockDontCareFiles(String... files) {
    ensureMock();
    mock.when(GisConfig::getDontCareFiles).thenReturn(files);
  }

  public static void mockModuleTimeoutSeconds(int seconds) {
    ensureMock();
    mock.when(GisConfig::getModuleTimeoutSeconds).thenReturn(seconds);
  }

  public static void close() {
    if (mock != null && !mock.isClosed()) {
      mock.close();
    }
  }
}
