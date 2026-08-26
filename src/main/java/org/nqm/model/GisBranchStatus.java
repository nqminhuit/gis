package org.nqm.model;

/**
 * The branch header line of `git status -sb`.
 *
 * @param name     current branch name, "HEAD" when detached
 * @param upstream the tracked remote branch, null when the branch tracks nothing
 * @param ahead    number of commits ahead of upstream
 * @param behind   number of commits behind upstream
 * @param gone     whether git reported the upstream as gone
 * @param detached whether HEAD is detached
 */
public record GisBranchStatus(
    String name, String upstream, int ahead, int behind, boolean gone, boolean detached) {}
