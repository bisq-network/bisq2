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

# Builds of tags that do not know -PbuildCommit (see below) read their commit hash with `git rev-parse`. Inside the
# repository they would get the commit of the checkout instead.
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

# The export has no .git, so the build takes its commit hash from this property. Builds of older tags ignore it.
gradle_args=("-PbuildCommit=$commit")
if [ -f "$overlay_dir/init.gradle" ]; then
    gradle_args+=(--init-script "$overlay_dir/init.gradle" "-Pbisq.compat.overlayDir=$overlay_dir")
fi

cd "$baseline_dir"
exec ./gradlew "${gradle_args[@]}" "$@"
