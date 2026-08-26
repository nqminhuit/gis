package org.nqm.command;

import static org.nqm.config.GisConfig.currentDir;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import org.nqm.model.GisModuleState;
import org.nqm.utils.GisJsonUtils;
import org.nqm.utils.StdOutUtils;

/**
 * Reports how far each module of the current command got. Every state change prints the state of
 * every module as one JSON document on stderr, so that stdout stays free for the command result.
 */
final class ModuleProgress {

  private static boolean enabled;

  /** the names to report, in the order the modules are run, or null when reporting is off */
  private final Map<Path, String> names;

  private final Map<Path, GisModuleState> states;

  private ModuleProgress(Map<Path, String> names) {
    this.names = names;
    this.states = names == null ? null : new LinkedHashMap<>();
  }

  static void setEnabled(boolean b) {
    enabled = b;
  }

  static ModuleProgress of(Collection<Path> modules) {
    if (!enabled) {
      return new ModuleProgress(null);
    }
    var progress = new ModuleProgress(namesOf(modules));
    modules.forEach(module -> progress.states.put(module, GisModuleState.PENDING));
    progress.report();
    return progress;
  }

  /**
   * Modules are named after their directory, like everywhere else in the app. Since the report is
   * keyed by that name, the few modules which would collide (a nested submodule sharing its
   * directory name with another one) are named after their path instead.
   */
  private static Map<Path, String> namesOf(Collection<Path> modules) {
    var rootDir = Path.of(currentDir());
    var taken = new HashSet<String>();
    var names = new LinkedHashMap<Path, String>();
    modules.forEach(module -> {
      var name = "" + module.getFileName();
      names.put(module, taken.add(name) ? name : "" + rootDir.relativize(module));
    });
    return names;
  }

  void inProgress(Path module) {
    set(module, GisModuleState.IN_PROGRESS);
  }

  void done(Path module) {
    set(module, GisModuleState.DONE);
  }

  void failed(Path module) {
    set(module, GisModuleState.FAILED);
  }

  /**
   * Fails every module which did not finish, e.g. because the run timed out.
   */
  synchronized void failUnfinished() {
    if (states == null) {
      return;
    }
    var unfinished = states.entrySet().stream()
        .filter(e -> e.getValue() != GisModuleState.DONE && e.getValue() != GisModuleState.FAILED)
        .toList();
    if (unfinished.isEmpty()) {
      return;
    }
    unfinished.forEach(e -> e.setValue(GisModuleState.FAILED));
    report();
  }

  private synchronized void set(Path module, GisModuleState state) {
    if (states == null || !states.containsKey(module)) {
      return;
    }
    states.put(module, state);
    report();
  }

  private void report() {
    var report = new LinkedHashMap<String, GisModuleState>();
    states.forEach((module, state) -> report.put(names.get(module), state));
    StdOutUtils.progressln(GisJsonUtils.toProgressJson(report));
  }
}
