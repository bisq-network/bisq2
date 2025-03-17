#!/usr/bin/env bash
set -e

source seed-debian-common.sh

echo "[*] Bisq bisq2-seed installation script"

echo "[*] Upgrading apt packages"
sudo -H -i -u "${ROOT_USER}" DEBIAN_FRONTEND=noninteractive apt update -q
sudo -H -i -u "${ROOT_USER}" DEBIAN_FRONTEND=noninteractive apt upgrade -qq -y

echo "[*] Installing OpenJDK 17"
sudo -H -i -u "${ROOT_USER}" apt install -qq -y openjdk-17-jdk

echo "[*] Installing Git"
sudo -H -i -u "${ROOT_USER}" apt install -qq -y git

echo "[*] Creating Bisq user"
sudo adduser --home "${BISQ_HOME}" --disabled-password --gecos "" "${BISQ_USER}" --quiet

echo "[*] Cloning Bisq repo"
sudo -H -i -u "${BISQ_USER}" git clone "${BISQ_REPO_URL}" "${BISQ_HOME}/${BISQ_REPO_NAME}"

update-set-branch "${BISQ_REPO_TAG_OR_BRANCH}"
update-clean-install-seed

echo "[*] Installing bisq2-seed systemd service"
sudo -H -i -u "${ROOT_USER}" install -c -o "${ROOT_USER}" -g "${ROOT_GROUP}" -m 644 "${BISQ_HOME}/${BISQ_REPO_NAME}/apps/seed-node-app/service-scripts/bisq2-seed.service" "${SYSTEMD_SERVICE_HOME}"
sudo -H -i -u "${ROOT_USER}" install -c -o "${ROOT_USER}" -g "${ROOT_GROUP}" -m 644 "${BISQ_HOME}/${BISQ_REPO_NAME}/apps/seed-node-app/service-scripts/bisq2-seed.env" "${SYSTEMD_ENV_HOME}"

echo '[*] Part 1 of service installation done'
echo "[*] To finalize installation, see README.md for part 2 and 3"

exit 0
