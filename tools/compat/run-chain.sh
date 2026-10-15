#!/usr/bin/env bash
# Runs the compatibility chain between a released version (the baseline) and the code of this checkout.
#
# Usage: tools/compat/run-chain.sh [<tag>] [<exchange directory>]
#
# The tag defaults to the newest baseline in tools/compat/baselines. The exchange directory defaults to
# build/compat/<tag>. The script deletes it at the start, and keeps it afterwards for inspection.
#
# The chain runs four steps in turn. Each step reads the files of the step before it.
#   1. The released version creates requests and stores (tools/compat/baselines/<tag>/application/...).
#   2. This version processes them, migrates the oracle data and creates its own requests
#      (application/src/test/java/bisq/application/compat/<tag with _ instead of .>/...).
#   3. The released version processes the requests of this version and relays them.
#   4. This version processes the copies which the released version relayed.
#
# Extra Gradle arguments for both builds can be set in COMPAT_GRADLE_ARGS, for example "--max-workers=2".
set -euo pipefail

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
repo_root=$(cd "$script_dir/../.." && pwd)

if [ $# -ge 1 ]; then
    tag=$1
else
    tag=$(ls "$script_dir/baselines" | sort -t. -k1,1 -k2,2n -k3,3n | tail -n 1)
fi
if [ ! -d "$script_dir/baselines/$tag" ]; then
    echo "No overlay for the baseline $tag in $script_dir/baselines" >&2
    exit 2
fi
exchange_dir=${2:-$repo_root/build/compat/$tag}
rm -rf "$exchange_dir"
mkdir -p "$exchange_dir"
exchange_dir=$(cd "$exchange_dir" && pwd)

read -r -a extra_args <<< "${COMPAT_GRADLE_ARGS:-}"
# The steps of this checkout for a baseline are in a package of its own, for example v2_1_13 for v2.1.13
new_side_package=bisq.application.compat.$(echo "$tag" | tr '.' '_')

run_old() {
    echo "=== Step $1 with $tag"
    "$script_dir/baseline.sh" "$tag" :application:test --tests "bisq.application.compat.Step$1*" \
        "-Pbisq.compat.exchangeDir=$exchange_dir" --console=plain ${extra_args[@]+"${extra_args[@]}"}
}

run_new() {
    echo "=== Step $1 with this checkout"
    (cd "$repo_root" && ./gradlew :application:compatTest --tests "$new_side_package.Step$1*" \
        "-Pbisq.compat.exchangeDir=$exchange_dir" --console=plain ${extra_args[@]+"${extra_args[@]}"})
}

run_old 1
run_new 2
run_old 3
run_new 4
echo "The compatibility chain with $tag passed. Files: $exchange_dir"
