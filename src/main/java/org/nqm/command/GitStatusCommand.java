package org.nqm.command;

import static org.nqm.model.FileStatus.ADDED;
import static org.nqm.model.FileStatus.CHANGED;
import static org.nqm.model.FileStatus.CONFLICT;
import static org.nqm.model.FileStatus.MISSING;
import static org.nqm.model.FileStatus.MODIFIED;
import static org.nqm.model.FileStatus.REMOVED;
import static org.nqm.model.FileStatus.UNTRACKED;
import static org.nqm.model.FileStatus.UNTRACKED_DIRS;
import static org.nqm.utils.GisStringUtils.getDirectoryName;
import static org.nqm.utils.StdOutUtils.CL_GREEN;
import static org.nqm.utils.StdOutUtils.CL_RED;
import static org.nqm.utils.StdOutUtils.coloringBranch;
import static org.nqm.utils.StdOutUtils.coloringWord;
import static org.nqm.utils.StdOutUtils.infof;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.submodule.SubmoduleWalk.IgnoreSubmoduleMode;
import org.nqm.model.FileStatus;
import org.nqm.utils.GisStringUtils;
import org.nqm.utils.StdOutUtils;

public class GitStatusCommand {

  private static Repository openRepo(Path f) throws IOException {
    var repo = new FileRepositoryBuilder().readEnvironment();
    if (f != null) {
      repo.findGitDir(f.toFile());
    } else {
      repo.findGitDir();
    }
    return repo.build();
  }

  private static String aheadBehind(Repository repo) throws IOException {
    var trackingStatus = BranchTrackingStatus.of(repo, repo.getBranch());
    int ahead = trackingStatus.getAheadCount();
    int behind = trackingStatus.getBehindCount();
    return StdOutUtils.buildAheadBehind(ahead, behind);
  }

  private static String statusOneLiner(Repository repo, Status status, String localBranch)
      throws IOException {
    // TODO: add styling modified and changed files
    // TODO: add styling for upstream?
    var result = new StringBuilder(" ")
        .append(coloringBranch(localBranch))
        .append(aheadBehind(repo));

    var updatedFiles = Stream.of(status.getModified().stream(),
        status.getChanged().stream(),
        status.getAdded().stream(),
        status.getMissing().stream(),
        status.getRemoved().stream(),
        status.getUntracked().stream(),
        status.getConflicting().stream(),
        status.getUntrackedFolders().stream())
        .reduce(Stream::concat)
        .orElseGet(Stream::empty)
        .distinct()
        .collect(Collectors.joining(" "));
    if (GisStringUtils.isNotBlank(updatedFiles)) {
      result.append(" ").append(updatedFiles);
    }
    return result.toString();
  }

  private static String statusFull(Repository repo, Status status, String localBranch) throws IOException {
    var trackingBranch = new BranchConfig(repo.getConfig(), localBranch)
        .getTrackingBranch()
        .replace("refs/remotes/", "");
    var result = new StringBuilder()
        .append(GisStringUtils.NEWLINE)
        .append("  ## ")
        .append(coloringWord(localBranch, CL_GREEN))
        .append("...")
        .append(coloringWord(trackingBranch, CL_RED))
        .append(" ")
        .append(aheadBehind(repo));

    var statusByFiles = new HashMap<String, List<FileStatus>>();

    Function<FileStatus, Consumer<String>> computeStatus = s -> f -> {
      var value = statusByFiles.computeIfAbsent(f, v -> new ArrayList<FileStatus>());
      value.add(s);
      statusByFiles.put(f, value);
    };

    // TODO how do we know of file rename status
    status.getModified().stream().forEach(computeStatus.apply(MODIFIED));
    status.getChanged().stream().forEach(computeStatus.apply(CHANGED));
    status.getAdded().stream().forEach(computeStatus.apply(ADDED));
    status.getMissing().stream().forEach(computeStatus.apply(MISSING));
    status.getRemoved().stream().forEach(computeStatus.apply(REMOVED));
    status.getUntracked().stream().forEach(computeStatus.apply(UNTRACKED));
    status.getConflicting().stream().forEach(computeStatus.apply(CONFLICT));
    status.getUntrackedFolders().stream().forEach(computeStatus.apply(UNTRACKED_DIRS));

    statusByFiles.forEach((k, v) -> result.append(GisStringUtils.NEWLINE)
        .append("  ")
        .append(FileStatus.toPrint(k, v)));
    return result.toString();
  }

  public static String status(Path f, boolean oneLiner) throws IOException, GitAPIException {
    var repo = openRepo(f.resolve(".git"));
    Status status;
    try (var git = new Git(repo)) {
      status = git.status()
          .setIgnoreSubmodules(IgnoreSubmoduleMode.ALL)
          .call();
    }
    if (status == null) {
      return "";
    }
    var result = new StringBuilder(infof(getDirectoryName(repo.getWorkTree().toPath())));
    var localBranch = repo.getBranch();
    result.append(oneLiner
        ? statusOneLiner(repo, status, localBranch)
        : statusFull(repo, status, localBranch));
    return result.toString();
  }
}
