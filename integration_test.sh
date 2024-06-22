#!/usr/bin/env sh

_gis_old() {
    gis $@
}

_gis_new() {
    /opt/gis $@
}

file_old="/tmp/gis_$(_gis_old --version)"
record_old() {
    (
        cd $HOME/projects/test2/small-git-root-module
        _gis_old --version > $file_old
        _gis_old checkout test1 >> $file_old
        _gis_old checkout master >> $file_old

        _gis_old checkout-branch "___iiTestii" >> $file_old
        _gis_old checkout "master" >> $file_old

        _gis_old fetch >> $file_old

        _gis_old fetch-origin master >> $file_old

        _gis_old files >> $file_old

        # gis init

        _gis_old branches >> $file_old
        _gis_old branches -nn >> $file_old

        _gis_old local-prune master >> $file_old

        _gis_old pull >> $file_old

        # push, pus
        # rebase-current-origin, ru
        # rebase-origin, re
        # remote-prune-origin, rpo

        yes | _gis_old remove-branch "___iiTestii" > /dev/null # not write file b/c this generate too many diffs

        _gis_old stash >> $file_old
        _gis_old stash -pp >> $file_old

        _gis_old >> $file_old
        _gis_old status >> $file_old
    )
    cat $file_old | sort > "$file_old"_sorted
    rm -rf $file_old
}

file_new="/tmp/gis_$(_gis_new --version)"
record_new() {
    (
        cd $HOME/projects/test2/small-git-root-module
        _gis_new --version > $file_new
        _gis_new checkout test1 >> $file_new
        _gis_new checkout master >> $file_new

        _gis_new checkout-branch "___iiTestii" >> $file_new
        _gis_new checkout "master" >> $file_new

        _gis_new fetch >> $file_new

        _gis_new fetch-origin master >> $file_new

        _gis_new files >> $file_new

        # gis init

        _gis_new branches >> $file_new
        _gis_new branches -nn >> $file_new

        _gis_new local-prune master >> $file_new

        _gis_new pull >> $file_new

        # push, pus
        # rebase-current-origin, ru
        # rebase-origin, re
        # remote-prune-origin, rpo

        yes | _gis_new remove-branch "___iiTestii" > /dev/null # not write file b/c this generate too many diffs

        _gis_new stash >> $file_new
        _gis_new stash -pp >> $file_new

        _gis_new >> $file_new
        _gis_new status >> $file_new
    )
    cat $file_new | sort > "$file_new"_sorted
    rm -rf $file_new
}

record_old
record_new
diff "$file_old"_sorted "$file_new"_sorted
