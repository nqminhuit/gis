package org.nqm.utils;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.nqm.model.GisBranchStatus;
import org.nqm.model.GisFileChange;
import org.nqm.model.GisModuleStatus;

class GisJsonUtilsTest {

  @Test
  void toJson_OK() {
    // given:
    var modules = List.of(
        new GisModuleStatus(
            "gis",
            true,
            new GisBranchStatus("master", "origin/master", 2, 9, false, false),
            List.of(
                new GisFileChange(" ", "M", "pom.xml", null),
                new GisFileChange("R", " ", "text-0001", "text-1"))),
        new GisModuleStatus("submodule1", false, new GisBranchStatus("HEAD", null, 0, 0, false, true), List.of()));

    // then:
    assertThat(GisJsonUtils.toJson(modules, "2024-05-06T07:08:09")).isEqualTo("""
        {
          "fetchedAt": "2024-05-06T07:08:09",
          "modules": [
            {
              "module": "gis",
              "root": true,
              "branch": {
                "name": "master",
                "upstream": "origin/master",
                "ahead": 2,
                "behind": 9,
                "gone": false,
                "detached": false
              },
              "files": [
                {
                  "indexStatus": " ",
                  "worktreeStatus": "M",
                  "path": "pom.xml",
                  "originalPath": null
                },
                {
                  "indexStatus": "R",
                  "worktreeStatus": " ",
                  "path": "text-0001",
                  "originalPath": "text-1"
                }
              ]
            },
            {
              "module": "submodule1",
              "root": false,
              "branch": {
                "name": "HEAD",
                "upstream": null,
                "ahead": 0,
                "behind": 0,
                "gone": false,
                "detached": true
              },
              "files": []
            }
          ]
        }""");
  }

  @Test
  void toJson_withoutModule_OK() {
    assertThat(GisJsonUtils.toJson(List.of(), null)).isEqualTo("""
        {
          "fetchedAt": null,
          "modules": []
        }""");
  }

  @Test
  void toJson_withNullBranch_OK() {
    // given:
    var modules = List.of(new GisModuleStatus("m", false, null, List.of()));

    // then:
    assertThat(GisJsonUtils.toJson(modules, null)).contains("\"branch\": null");
  }

  @Test
  void quote_shouldEscapeSpecialCharacters() {
    assertThat(GisJsonUtils.quote(null)).isEqualTo("null");
    assertThat(GisJsonUtils.quote("a\"b")).isEqualTo("\"a\\\"b\"");
    assertThat(GisJsonUtils.quote("a\\b")).isEqualTo("\"a\\\\b\"");
    assertThat(GisJsonUtils.quote("a\nb\tc\rd")).isEqualTo("\"a\\nb\\tc\\rd\"");
    assertThat(GisJsonUtils.quote("a\u0001b")).isEqualTo("\"a\\u0001b\"");
    assertThat(GisJsonUtils.quote("tệp")).isEqualTo("\"tệp\"");
  }
}
