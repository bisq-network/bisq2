#!/usr/bin/env bash
# Checks that JitPack serves the same jars as a local build of the same commit.
#
# Usage:
#   .github/scripts/check-jitpack-reproducibility.bash request <owner/repository> <commit>
#   .github/scripts/check-jitpack-reproducibility.bash compare <local repository> <owner/repository> <commit> <work directory>
#
# request starts the JitPack build of the commit and returns without waiting for it.
#
# compare requests the build, waits until JitPack has finished it, downloads each jar of the local repository under
# the coordinates which JitPack gives it and compares the bytes. It copies differing jars to
# <work directory>/different/local and <work directory>/different/jitpack. It compares only jars, because JitPack
# rewrites the .pom and .module files. JitPack also adds -sources jars of its own, which the comparison ignores.
#
# The local repository must be empty before the command of jitpack.yml publishes into it, for example:
#   ./gradlew --no-build-cache -Dmaven.repo.local=/tmp/local-repo publishAll
# Run Gradle on a JDK with a bundled zlib, such as Temurin. jitpack.yml explains why.
#
# JITPACK_TIMEOUT_SECONDS sets how long compare waits for the JitPack build (default 2700).
#
# Exit codes: 0 all jars are identical, 1 a jar differs or JitPack does not serve it, 2 the JitPack build failed or did
# not finish in time, 3 wrong usage or no jars in the local repository.
set -euo pipefail

readonly JITPACK=https://jitpack.io
readonly POLL_SECONDS=30
# A failed status is not always final. JitPack retried a build which a full disk on its machine had stopped, and the
# status changed from "Error" to "ok" after 5 minutes (2026-10-19). A commit which JitPack has not fetched yet
# reports "tagNotFound".
readonly FAILED_GRACE_SECONDS=900

usage() {
    sed -n '4,6p' "${BASH_SOURCE[0]}" | sed 's/^# *//' >&2
    exit 3
}

# Sets owner, repository_name, version and group_path. The values become parts of URLs, so they must be plain names.
parse_coordinates() {
    if [[ ! $1 =~ ^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$ ]]; then
        echo "Invalid repository: $1" >&2
        exit 3
    fi
    if [[ ! $2 =~ ^[0-9a-f]{10,40}$ ]]; then
        echo "Invalid commit: $2" >&2
        exit 3
    fi
    owner=${1%%/*}
    repository_name=${1#*/}
    # JitPack names the version of a commit after its first 10 characters
    version=${2:0:10}
    group_path=com/github/$owner/$repository_name
}

# Requesting an artifact starts the build. JitPack holds the request open while it builds, so the timeout and the
# response are expected and ignored.
request_build() {
    curl --silent --output /dev/null --max-time 30 "$JITPACK/$group_path/$version/$repository_name-$version.pom" || true
}

wait_for_build() {
    local api_url=$JITPACK/api/builds/com.github.$owner/$repository_name/$version
    local deadline=$((SECONDS + ${JITPACK_TIMEOUT_SECONDS:-2700}))
    local response status failed_since=
    echo "Waiting for the JitPack build $owner/$repository_name $version"
    while :; do
        # A failed request counts as a build in progress
        response=$(curl --silent --show-error --fail --max-time 60 "$api_url") || response='{"status":"none"}'
        status=$(jq -r '.status // "none"' <<< "$response")
        case $status in
            ok)
                return
                ;;
            none | Queued | Building)
                failed_since=
                ;;
            *)
                failed_since=${failed_since:-$SECONDS}
                if [ $((SECONDS - failed_since)) -ge "$FAILED_GRACE_SECONDS" ]; then
                    echo "The JitPack build failed with the status $status: $(jq -r '.message // ""' <<< "$response")"
                    echo "Build log: $JITPACK/$group_path/$version/build.log"
                    exit 2
                fi
                ;;
        esac
        if [ "$SECONDS" -ge "$deadline" ]; then
            echo "The JitPack build did not finish in time. Last status: $status"
            echo "Build log: $JITPACK/$group_path/$version/build.log"
            exit 2
        fi
        sleep "$POLL_SECONDS"
    done
}

# Sets jars to the paths of the jars in the local repository, relative to it
list_jars() {
    if [ -d "$1" ]; then
        mapfile -t jars < <(cd "$1" && find . -type f -name '*.jar' | sed 's|^\./||' | LC_ALL=C sort)
    fi
    if [ ${#jars[@]} -eq 0 ]; then
        echo "No jars in $1" >&2
        exit 3
    fi
}

compare_jars() {
    local local_repo=$1 work_dir=$2
    local path file artifact local_version suffix jitpack_file download
    local identical=0 failed=0
    declare -A local_path_of

    mkdir -p "$work_dir/jitpack" "$work_dir/different/local" "$work_dir/different/jitpack"

    for path in "${jars[@]}"; do
        # Maven layout: <group path>/<artifact>/<version>/<artifact>-<version>[-<classifier>].jar
        file=${path##*/}
        local_version=$(basename "$(dirname "$path")")
        artifact=$(basename "$(dirname "$(dirname "$path")")")
        if [[ $file != "$artifact-$local_version"* ]]; then
            echo "UNEXPECTED  $path does not follow the Maven layout"
            failed=$((failed + 1))
            continue
        fi
        suffix=${file#"$artifact-$local_version"}
        # JitPack moves every artifact into one group, so artifacts with the same name in different groups collide
        jitpack_file=$artifact-$version$suffix
        if [ -n "${local_path_of[$jitpack_file]:-}" ]; then
            echo "COLLISION   $path and ${local_path_of[$jitpack_file]} both become $jitpack_file on JitPack"
            failed=$((failed + 1))
            continue
        fi
        local_path_of[$jitpack_file]=$path

        download=$work_dir/jitpack/$jitpack_file
        if ! curl --silent --fail --location --retry 3 --retry-all-errors --max-time 300 --output "$download" \
                "$JITPACK/$group_path/$artifact/$version/$jitpack_file"; then
            echo "MISSING     $artifact$suffix is not on JitPack"
            failed=$((failed + 1))
            continue
        fi
        if cmp --silent "$local_repo/$path" "$download"; then
            echo "IDENTICAL   $artifact$suffix"
            identical=$((identical + 1))
        else
            echo "DIFFERENT   $artifact$suffix local $(sha256sum < "$local_repo/$path" | cut -c1-16)" \
                "JitPack $(sha256sum < "$download" | cut -c1-16)"
            cp "$local_repo/$path" "$work_dir/different/local/$file"
            cp "$download" "$work_dir/different/jitpack/$jitpack_file"
            failed=$((failed + 1))
        fi
    done

    echo "$identical of ${#jars[@]} jars are identical with the JitPack jars of $owner/$repository_name $version."
    [ "$failed" -eq 0 ] || exit 1
}

case ${1:-} in
    request)
        [ $# -eq 3 ] || usage
        parse_coordinates "$2" "$3"
        request_build
        ;;
    compare)
        [ $# -eq 5 ] || usage
        jars=()
        list_jars "$2"
        parse_coordinates "$3" "$4"
        request_build
        wait_for_build
        compare_jars "$2" "$5"
        ;;
    *)
        usage
        ;;
esac
