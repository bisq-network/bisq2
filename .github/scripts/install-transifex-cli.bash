#!/usr/bin/env bash
set -euo pipefail

# CI installer for GitHub's Ubuntu runners; extend the OS table before reusing it elsewhere.
readonly TRANSIFEX_CLI_VERSION="v1.6.17"
readonly TRANSIFEX_CLI_VERSION_NUMBER="${TRANSIFEX_CLI_VERSION#v}"
readonly TX_LINUX_AMD64_SHA256="002dec5b9e71248a7e6a0808118e9da940205828d5a33ce88e04bb57a967164d"
readonly TX_LINUX_ARM64_SHA256="c380ed9aece5d34316aa0fe2e5afa0633553656f98909f88cb72609e2fa13153"

case "$(uname -s)" in
    Linux)
        os="linux"
        ;;
    *)
        echo "::error::Unsupported operating system for Transifex CLI installation: $(uname -s)"
        exit 1
        ;;
esac

case "$(uname -m)" in
    x86_64 | amd64)
        arch="amd64"
        checksum="$TX_LINUX_AMD64_SHA256"
        ;;
    aarch64 | arm64)
        arch="arm64"
        checksum="$TX_LINUX_ARM64_SHA256"
        ;;
    *)
        echo "::error::Unsupported architecture for Transifex CLI installation: $(uname -m)"
        exit 1
        ;;
esac

asset="tx-${os}-${arch}.tar.gz"
download_url="https://github.com/transifex/cli/releases/download/${TRANSIFEX_CLI_VERSION}/${asset}"
install_dir="${RUNNER_TEMP:-${TMPDIR:-/tmp}}/tx-cli"
archive="${install_dir}/${asset}"
extract_dir="${install_dir}/extract"

mkdir -p "$install_dir"
rm -rf "$extract_dir"
mkdir -p "$extract_dir"

echo "::group::Install Transifex CLI ${TRANSIFEX_CLI_VERSION}"
curl --fail --location --show-error --silent "$download_url" --output "$archive"
printf '%s  %s\n' "$checksum" "$archive" | sha256sum -c -
tar -xzf "$archive" -C "$extract_dir"
rm -f "$archive"
install -m 0755 "${extract_dir}/tx" "${install_dir}/tx"
rm -rf "$extract_dir"

actual_version="$("${install_dir}/tx" --version)"
expected_version="TX Client, version=${TRANSIFEX_CLI_VERSION_NUMBER}"
echo "Installed Transifex CLI: $actual_version"
if [[ "$actual_version" != "$expected_version" ]]; then
    echo "::error::Unexpected Transifex CLI version: $actual_version"
    exit 1
fi

if [[ -n "${GITHUB_PATH:-}" ]]; then
    # GITHUB_PATH affects later workflow steps; this script uses the absolute tx path above.
    echo "$install_dir" >> "$GITHUB_PATH"
fi
echo "::endgroup::"
