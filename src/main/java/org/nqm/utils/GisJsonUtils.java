package org.nqm.utils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.nqm.model.GisBranchStatus;
import org.nqm.model.GisFileChange;
import org.nqm.model.GisModuleStatus;

/**
 * Renders the status model as JSON. Hand written on purpose: the app prefers the Java core over
 * extra dependencies, and the model to serialize is tiny and fully known.
 */
public class GisJsonUtils {

  private GisJsonUtils() {}

  private static final String INDENT = "  ";
  private static final String NULL = "null";
  private static final String EMPTY_ARRAY = "[]";

  public static String toJson(Collection<GisModuleStatus> modules, String fetchedAt) {
    return object(0,
        field("fetchedAt", quote(fetchedAt)),
        field("modules", array(1, modules.stream().map(m -> module(2, m)).toList())));
  }

  private static String module(int level, GisModuleStatus module) {
    return object(level,
        field("module", quote(module.module())),
        field("root", "" + module.root()),
        field("branch", module.branch() == null ? NULL : branch(level + 1, module.branch())),
        field("files", array(level + 1, module.files().stream().map(f -> file(level + 2, f)).toList())));
  }

  private static String branch(int level, GisBranchStatus branch) {
    return object(level,
        field("name", quote(branch.name())),
        field("upstream", quote(branch.upstream())),
        field("ahead", "" + branch.ahead()),
        field("behind", "" + branch.behind()),
        field("gone", "" + branch.gone()),
        field("detached", "" + branch.detached()));
  }

  private static String file(int level, GisFileChange file) {
    return object(level,
        field("indexStatus", quote(file.indexStatus())),
        field("worktreeStatus", quote(file.worktreeStatus())),
        field("path", quote(file.path())),
        field("originalPath", quote(file.originalPath())));
  }

  private static String field(String name, String value) {
    return "%s: %s".formatted(quote(name), value);
  }

  /**
   * Renders an object whose closing brace sits at {@code level} and whose fields sit one level
   * deeper. The opening brace is left where the caller already placed it.
   */
  private static String object(int level, String... fields) {
    return wrap(level, "{", "}", Stream.of(fields).toList());
  }

  private static String array(int level, List<String> items) {
    return items.isEmpty() ? EMPTY_ARRAY : wrap(level, "[", "]", items);
  }

  private static String wrap(int level, String open, String close, List<String> items) {
    var inner = INDENT.repeat(level + 1);
    return "%s%n%s%s%n%s%s".formatted(
        open, inner, String.join(",%n%s".formatted(inner), items), INDENT.repeat(level), close);
  }

  public static String quote(String value) {
    if (value == null) {
      return NULL;
    }
    var sb = new StringBuilder("\"");
    value.codePoints().forEach(c -> sb.append(escape(c)));
    return sb.append('"').toString();
  }

  private static String escape(int c) {
    return switch (c) {
      case '"' -> "\\\"";
      case '\\' -> "\\\\";
      case '\b' -> "\\b";
      case '\f' -> "\\f";
      case '\n' -> "\\n";
      case '\r' -> "\\r";
      case '\t' -> "\\t";
      default -> c < 0x20 ? "\\u%04x".formatted(c) : Character.toString(c);
    };
  }
}
