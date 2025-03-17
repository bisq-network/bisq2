#!/usr/bin/env bash
set -e

source oracle-debian-common.sh

show-usage-and-exit () {
  echo -e "
  Usage:

     oracle-debian-update --main

              Re-builds oracle for the latest version of branch main on bisq-network/bisq2.

              Steps in detail:
              - Switches the remote URL to https://github.com/bisq-network/bisq2
              - Switches the branch to main
              - Pulls the latest changes
              - Re-builds the oracle binaries

              Typical scenario: several PRs have been merged in the main repo and we want to update to the latest commit.

     oracle-debian-update --current

              Re-builds oracle for the latest version of the current branch, from the currently configured remote.
              Current branch and current remote can be seen with --get-status
              Equivalent to --main, if the local branch has never been changed via --pr

              Steps in detail:
              - Pulls the latest changes (for the current branch on the current remote)
              - Re-builds the oracle binaries

              Typical scenario: the current branch received new commits and we want to update the oracle to the latest commit.
              Especially useful with PR branches, where the PR just received new commits and we want to re-build th oracle.

     oracle-debian-update --pr <pr-repo> <pr-branch>

              where
                    <pr-repo>    is the GitHub user which hosts a bisq2 clone, typically the creator of a PR
                    <pr-branch>  is the desired branch from the specified repo, typically the PR branch

              Re-builds oracle for the latest version of the specified branch from the specified repo.

              Steps in detail:
              - Switches the remote URL to https://github.com/<pr-repo>/bisq2
              - Switches the branch to <pr-branch>
              - Pulls the latest changes
              - Re-builds the oracle binaries

              Typical scenario: we want to build the oracle binary for a specific PR.

              Example: oracle-debian-update --pr alkum my-branch-123

     oracle-debian-update --get-status

              Shows current branch and current remote configured in the local git repo.
   "

   exit
}

case $1 in
    --main)
        if (( $# != 1 )); then
            >&2 echo "Illegal number of arguments"
            show-usage-and-exit
        fi

        sudo systemctl stop bisq2-oracle
        update-set-remote bisq-network
        update-set-branch main
        update-pull-current-branch
        update-clean-install-oracle
        update-show-status
        sudo systemctl start bisq2-oracle
    ;;
    --current)
        if (( $# != 1 )); then
            >&2 echo "Illegal number of arguments"
            show-usage-and-exit
        fi

        sudo systemctl stop bisq2-oracle
        update-pull-current-branch
        update-clean-install-oracle
        update-show-status
        sudo systemctl start bisq2-oracle
    ;;
    --pr)
        if (( $# != 3 )); then
            >&2 echo "Illegal number of arguments"
            show-usage-and-exit
        fi

        sudo systemctl stop bisq2-oracle
        update-set-remote $2
        update-set-branch $3
        update-pull-current-branch
        update-clean-install-oracle
        update-show-status
        sudo systemctl start bisq2-oracle
    ;;
    --get-status)
        if (( $# != 1 )); then
            >&2 echo "Illegal number of arguments"
            show-usage-and-exit
        fi

        update-show-status
    ;;
    *)
        show-usage-and-exit
    ;;
esac

exit 0
