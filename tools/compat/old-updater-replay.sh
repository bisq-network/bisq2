#!/usr/bin/env bash
# Runs the verification code of the in-app updater of a released version (default v2.1.13) against the keys and the
# release assets of a new release. Users of that version update with this code.
#
# Usage:
#   tools/compat/old-updater-replay.sh keys [<key dir>]
#       Checks that the old version reads the key files 387C8307.asc and E222AA02.asc. The key dir defaults to
#       evolution/src/main/resources/keys of this checkout.
#   tools/compat/old-updater-replay.sh signature <file> [<key id>]
#       Verifies <file>.asc, a detached signature of <file>, with the old code. It uses the key file of this checkout
#       and the key file which the old version has in its resources. Create the signature with
#       gpg --local-user <key id> --detach-sign --armor <file>. The key id defaults to 387C8307.
#   tools/compat/old-updater-replay.sh release <version> [<staging dir>] [<extensions>]
#       Downloads each installer of the release like the old updater and verifies it with the old code. Without a
#       staging dir it uses the GitHub release and https://bisq.network/pubkey/. A staging dir contains the release
#       assets and the web page keys in the subdirectory pubkey, so the release can be checked before the upload.
#       Extensions default to .dmg,.exe,.deb,.rpm. The installers are large, so the download takes a while.
#
# The baseline tag can be set with COMPAT_BASELINE (default v2.1.13).
set -euo pipefail

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
repo_root=$(cd "$script_dir/../.." && pwd)
tag=${COMPAT_BASELINE:-v2.1.13}
baseline_dir=${BISQ_BASELINE_ROOT:-$HOME/.cache/bisq2/baselines}/$tag

run() {
    local log status
    log=$(mktemp)
    set +e
    "$script_dir/baseline.sh" "$tag" :evolution:test --tests 'bisq.evolution.updater.OldUpdaterReplayTest' \
        --console=plain -i "$@" > "$log" 2>&1
    status=$?
    set -e
    grep -E "OldUpdaterReplayTest >|\.asc: \[|accepts the download|BUILD" "$log" || true
    if [ "$status" -ne 0 ]; then
        grep -E -A12 "OldUpdaterReplayTest > .* FAILED" "$log" | grep -v "^\s*at " || true
        echo "The check failed. Full log: $log"
    else
        rm -f "$log"
    fi
    return "$status"
}

absolute() {
    (cd "$(dirname "$1")" && echo "$(pwd)/$(basename "$1")")
}

if [ $# -lt 1 ]; then
    sed -n '2,22p' "$0"
    exit 2
fi
mode=$1
shift

case "$mode" in
    keys)
        key_dir=$(absolute "${1:-$repo_root/evolution/src/main/resources/keys}")
        run "-Pbisq.compat.keyDir=$key_dir"
        ;;
    signature)
        [ $# -ge 1 ] || { echo "Usage: $0 signature <file> [<key id>]" >&2; exit 2; }
        signed_file=$(absolute "$1")
        key_id=${2:-387C8307}
        key_dirs="$repo_root/evolution/src/main/resources/keys,$baseline_dir/evolution/src/main/resources/keys"
        run "-Pbisq.compat.signedFile=$signed_file" "-Pbisq.compat.signingKeyId=$key_id" "-Pbisq.compat.keyDirs=$key_dirs"
        ;;
    release)
        [ $# -ge 1 ] || { echo "Usage: $0 release <version> [<staging dir>] [<extensions>]" >&2; exit 2; }
        version=$1
        download_dir=$repo_root/build/compat/old-updater-replay/$version
        mkdir -p "$download_dir"
        args=("-Pbisq.compat.releaseVersion=$version" "-Pbisq.compat.downloadDir=$download_dir")
        if [ $# -ge 2 ] && [ -n "$2" ]; then
            args+=("-Pbisq.compat.stagingDir=$(absolute "$2")")
        fi
        if [ $# -ge 3 ]; then
            args+=("-Pbisq.compat.installerExtensions=$3")
        fi
        run "${args[@]}"
        ;;
    *)
        echo "Unknown mode $mode. Use keys, signature or release." >&2
        exit 2
        ;;
esac
