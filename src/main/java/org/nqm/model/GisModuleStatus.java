package org.nqm.model;

import java.util.List;

/**
 * Status of a single module (either the root repository or one of its submodules).
 *
 * @param module the module directory name
 * @param root   whether this is the root repository
 * @param branch branch details, null when git reported no branch line
 * @param files  the modified/untracked files of that module
 */
public record GisModuleStatus(String module, boolean root, GisBranchStatus branch, List<GisFileChange> files) {}
