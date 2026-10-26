#!/usr/bin/env bash
# Runs Gradle in the source tree of a released version, the baseline of a compatibility test.
#
# Usage: tools/compat/baseline.sh <tag> [gradle arguments...]
#
# The script exports the tag with `git archive` into $BISQ_BASELINE_ROOT/<tag> (default ~/.cache/bisq2/baselines).
# It exports a tag only once and reuses the export and its build outputs on later runs. It creates no git worktree.
#
# If tools/compat/baselines/<tag>/init.gradle exists, Gradle runs with this init script. The init script adds the
# test sources of tools/compat/baselines/<tag> to the baseline build, so the exported tree stays unchanged. Gradle
# properties named bisq.compat.* reach the tests of these sources as system properties.
#
# Example:
#   tools/compat/baseline.sh v2.1.13 :application:test --tests 'bisq.application.compat.*' \
#       -Pbisq.compat.exchangeDir=/path/to/exchange
set -euo pipefail

if [ $# -lt 1 ]; then
    echo "Usage: $0 <tag> [gradle arguments...]" >&2
    exit 2
fi
tag=$1
shift

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
repo_root=$(cd "$script_dir/../.." && pwd)
baseline_root=${BISQ_BASELINE_ROOT:-$HOME/.cache/bisq2/baselines}
baseline_dir=$baseline_root/$tag
overlay_dir=$script_dir/baselines/$tag

if ! commit=$(git -C "$repo_root" rev-parse --verify --quiet "refs/tags/$tag^{commit}"); then
    echo "The tag $tag does not exist in $repo_root. Fetch it with: git fetch origin tag $tag --no-tags" >&2
    exit 2
fi

# The build of the baseline reads its commit hash with `git rev-parse`. Inside the repository it would get the commit
# of the checkout instead.
mkdir -p "$baseline_root"
baseline_root_real=$(cd "$baseline_root" && pwd -P)
repo_root_real=$(cd "$repo_root" && pwd -P)
case "$baseline_root_real/" in
    "$repo_root_real"/*)
        echo "BISQ_BASELINE_ROOT must be outside of the repository $repo_root_real." >&2
        exit 2
        ;;
esac

marker=$baseline_dir/.bisq-baseline-commit
if [ ! -f "$marker" ] || [ "$(cat "$marker")" != "$commit" ]; then
    echo "Exporting $tag ($commit) to $baseline_dir" >&2
    rm -rf "$baseline_dir"
    mkdir -p "$baseline_dir"
    git -C "$repo_root" archive --format=tar "$commit" | tar -x -C "$baseline_dir"
    # `git archive` leaves the directory of a submodule (tor/ since it is the Git submodule tor) empty. The commit of
    # each submodule must exist in its checkout.
    git -C "$repo_root" ls-tree -r "$commit" |
        while read -r _ object_type submodule_commit submodule_path; do
            [ "$object_type" = commit ] || continue
            # In an empty submodule directory, git would run in the superproject and report the wrong repository.
            if [ ! -e "$repo_root/$submodule_path/.git" ]; then
                echo "The submodule $submodule_path is not initialized. Run: git submodule update --init" >&2
                exit 2
            fi
            if ! git -C "$repo_root/$submodule_path" cat-file -e "$submodule_commit^{commit}" 2> /dev/null; then
                echo "The commit $submodule_commit of the submodule $submodule_path is missing. Fetch it with:" \
                    "git -C $submodule_path fetch origin $submodule_commit" >&2
                exit 2
            fi
            git -C "$repo_root/$submodule_path" archive --format=tar "$submodule_commit" |
                tar -x -C "$baseline_dir/$submodule_path"
        done
    echo "$commit" > "$marker"
fi

# The script runs the Gradle wrapper of the tag, so it checks the wrapper first, as the CI workflows do
if [ -f "$baseline_dir/gradle/wrapper/gradle-wrapper.sha256" ]; then
    if command -v sha256sum > /dev/null 2>&1; then
        (cd "$baseline_dir" && sha256sum --quiet -c gradle/wrapper/gradle-wrapper.sha256)
    else
        (cd "$baseline_dir" && shasum -a 256 --quiet -c gradle/wrapper/gradle-wrapper.sha256)
    fi
fi

gradle_args=()
if [ -f "$overlay_dir/init.gradle" ]; then
    gradle_args+=(--init-script "$overlay_dir/init.gradle" "-Pbisq.compat.overlayDir=$overlay_dir")
fi

cd "$baseline_dir"
# The expansion with + keeps `set -u` working with an empty array in bash 3.2 of macOS.
exec ./gradlew ${gradle_args[@]+"${gradle_args[@]}"} "$@"
