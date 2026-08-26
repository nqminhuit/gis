package org.nqm.model;

/**
 * One entry of `git status --porcelain=v1`.
 *
 * @param indexStatus   status of the index (the 'X' column), "?" when untracked
 * @param worktreeStatus status of the work tree (the 'Y' column), " " when unmodified
 * @param path          the path as reported by git, or the destination of a rename/copy
 * @param originalPath  the source of a rename/copy, null otherwise
 */
public record GisFileChange(String indexStatus, String worktreeStatus, String path, String originalPath) {}
