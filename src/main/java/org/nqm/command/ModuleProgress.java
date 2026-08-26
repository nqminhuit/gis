package org.nqm.command;

import static org.nqm.config.GisConfig.currentDir;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import org.nqm.model.GisModuleState;
import org.nqm.utils.GisJsonUtils;
import org.nqm.utils.GisStringUtils;
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

  /**
   * Reporting writes to stderr while holding the lock, to keep the reports in the order the
   * states changed. A `synchronized` block would pin the virtual thread of the module to its
   * carrier thread for that whole write, while a lock of java.util.concurrent lets it unmount.
   */
  private final ReentrantLock lock = new ReentrantLock();

  /** set once the run gave up on the unfinished modules, no state may change afterwards */
  private boolean aborted;

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

  private static Map<Path, String> namesOf(Collection<Path> modules) {
    var rootDir = Path.of(currentDir());
    var taken = new HashSet<String>();
    var names = new LinkedHashMap<Path, String>();
    modules.forEach(module -> names.put(module, uniqueName(module, rootDir, taken)));
    return names;
  }

  /**
   * Modules are named after their directory, like everywhere else in the app. Since the report is
   * keyed by that name, a module which would collide with an already named one (e.g. a nested
   * submodule sharing its directory name) falls back to its path, which no other module can hold.
   */
  private static String uniqueName(Path module, Path rootDir, Set<String> taken) {
    var candidates = List.of("" + module.getFileName(), "" + rootDir.relativize(module), "" + module);
    for (var candidate : candidates) {
      if (GisStringUtils.isNotBlank(candidate) && taken.add(candidate)) {
        return candidate;
      }
    }
    // the marker file lists the very same directory twice, so one entry does describe both
    return "" + module;
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
   * Fails every module which did not finish, e.g. because the run timed out. The states are
   * final afterwards: a module aborted mid flight may still be about to report itself done.
   */
  void failUnfinished() {
    if (states == null) {
      return;
    }
    lock.lock();
    try {
      if (aborted) {
        return;
      }
      aborted = true;
      var unfinished = states.entrySet().stream()
          .filter(e -> e.getValue() != GisModuleState.DONE && e.getValue() != GisModuleState.FAILED)
          .toList();
      if (unfinished.isEmpty()) {
        return;
      }
      unfinished.forEach(e -> e.setValue(GisModuleState.FAILED));
      report();
    } finally {
      lock.unlock();
    }
  }

  private void set(Path module, GisModuleState state) {
    // reporting is off, which is the default: the modules never contend on anything
    if (states == null) {
      return;
    }
    lock.lock();
    try {
      if (aborted || !states.containsKey(module)) {
        return;
      }
      states.put(module, state);
      report();
    } finally {
      lock.unlock();
    }
  }

  private void report() {
    var report = new LinkedHashMap<String, GisModuleState>();
    states.forEach((module, state) -> report.put(names.get(module), state));
    StdOutUtils.progressln(GisJsonUtils.toProgressJson(report));
  }
}
