#!/usr/bin/env bash
# Runs the tests of apps/e2e-tests: a network on 127.0.0.1 with nodes of a released version (the baseline) and of this
# checkout, which trade and exchange data through the API of the nodes.
#
# Usage: tools/compat/run-mixed-network.sh [<tag>] [<test filter>]
#
# The tag defaults to the newest baseline in tools/compat/baselines. The test filter is a Gradle --tests pattern, for
# example 'bisq.e2e.OffersAcrossVersionsTest'. The data directories and logs of the nodes stay in
# build/e2e/<tag>/<test class> (or $BISQ_E2E_WORK_DIR). Extra Gradle arguments for all builds can be set in
# COMPAT_GRADLE_ARGS, for example "--max-workers=2".
set -euo pipefail

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
repo_root=$(cd "$script_dir/../.." && pwd)

if [ $# -ge 1 ] && [ -n "$1" ]; then
    tag=$1
else
    tag=$(ls "$script_dir/baselines" | sort -t. -k1,1 -k2,2n -k3,3n | tail -n 1)
fi
filter=${2:-}
baseline_dir=${BISQ_BASELINE_ROOT:-$HOME/.cache/bisq2/baselines}/$tag
work_dir=${BISQ_E2E_WORK_DIR:-$repo_root/build/e2e/$tag}
read -r -a extra_args <<< "${COMPAT_GRADLE_ARGS:-}"

echo "=== Distributions of $tag"
"$script_dir/baseline.sh" "$tag" :apps:api-app:installDist :apps:seed-node-app:installDist --console=plain \
    ${extra_args[@]+"${extra_args[@]}"}

echo "=== Distributions of this checkout"
(cd "$repo_root" && ./gradlew :apps:api-app:installDist :apps:seed-node-app:installDist --console=plain \
    ${extra_args[@]+"${extra_args[@]}"})

echo "=== Tests"
test_args=()
if [ -n "$filter" ]; then
    test_args=(--tests "$filter")
fi
mkdir -p "$work_dir"
(cd "$repo_root" && ./gradlew :apps:e2e-tests:integrationTest ${test_args[@]+"${test_args[@]}"} \
    "-Pbisq.e2e.newTree=$repo_root" "-Pbisq.e2e.oldTree=$baseline_dir" "-Pbisq.e2e.oldVersion=$tag" \
    "-Pbisq.e2e.workDir=$work_dir" --console=plain ${extra_args[@]+"${extra_args[@]}"})
echo "The tests with $tag passed. Data directories and logs: $work_dir"
