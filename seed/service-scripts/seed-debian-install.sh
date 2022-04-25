#!/usr/bin/env bash
set -e

source seed-debian-common.sh

echo "[*] Bisq bisq2-seed installation script"

echo "[*] Upgrading apt packages"
sudo -H -i -u "${ROOT_USER}" DEBIAN_FRONTEND=noninteractive apt update -q
sudo -H -i -u "${ROOT_USER}" DEBIAN_FRONTEND=noninteractive apt upgrade -qq -y

echo "[*] Installing OpenJDK 17"
sudo -H -i -u "${ROOT_USER}" apt install -qq -y openjdk-17-jdk

echo "[*] Creating Bisq user"
sudo adduser --home "${BISQ_HOME}" --disabled-password --gecos "" "${BISQ_USER}" --quiet

echo "[*] Cloning Bisq repo"
sudo -H -i -u "${BISQ_USER}" git clone "${BISQ_REPO_URL}" "${BISQ_HOME}/${BISQ_REPO_NAME}"

update-set-branch "${BISQ_REPO_TAG_OR_BRANCH}"
update-clean-install-seed

echo "[*] Installing bisq2-seed systemd service"
sudo -H -i -u "${ROOT_USER}" install -c -o "${ROOT_USER}" -g "${ROOT_GROUP}" -m 644 "${BISQ_HOME}/${BISQ_REPO_NAME}/seed/service-scripts/bisq2-seed.service" "${SYSTEMD_SERVICE_HOME}"

echo "[*] Reloading systemd daemon configuration"
sudo -H -i -u "${ROOT_USER}" systemctl daemon-reload

echo "[*] Enabling bisq2-seed service"
sudo -H -i -u "${ROOT_USER}" systemctl enable bisq2-seed

echo "[*] Starting bisq2-seed service"
sudo -H -i -u "${ROOT_USER}" systemctl start bisq2-seed
sleep 5
sudo -H -i -u "${ROOT_USER}" journalctl --no-pager --unit bisq2-seed

echo '[*] Done'
echo "[*] Start with: sudo systemctl start bisq2-seed"
echo "[*] Stop with:  sudo systemctl stop bisq2-seed"

exit 0
