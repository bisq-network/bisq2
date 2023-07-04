#!/usr/bin/env bash
set -e

SYSTEMD_SERVICE_HOME=/etc/systemd/system
SYSTEMD_ENV_HOME=/etc/default

ROOT_USER=root
ROOT_GROUP=root

BISQ_USER=bisq
BISQ_GROUP=bisq
BISQ_HOME=/bisq

BISQ_REPO_NAME=bisq2
BISQ_REMOTE_NAME=bisq-network # Name of GitHub user or team from which the project is checked out
BISQ_REPO_URL="https://github.com/$BISQ_REMOTE_NAME/$BISQ_REPO_NAME"
BISQ_REPO_TAG_OR_BRANCH=main


update-clean-install-seed () {
  echo "[*] Building Bisq Seed from source"
  sudo -u "${BISQ_USER}" sh -c "cd ${BISQ_HOME}/${BISQ_REPO_NAME} && ./gradlew clean :seed:installDist -x test"
}

# Takes 1 argument (BISQ_REMOTE_NAME)
update-set-remote () {
  BISQ_REMOTE_NAME=$1 # Name of GitHub user or team from which the project is checked out
  BISQ_REPO_URL="https://github.com/$BISQ_REMOTE_NAME/$BISQ_REPO_NAME"

  echo "[*] Updating remote to ${BISQ_REMOTE_NAME}"
  sudo -H -i -u "${BISQ_USER}" sh -c "cd ${BISQ_HOME}/${BISQ_REPO_NAME} && git remote set-url origin ${BISQ_REPO_URL}"

  echo "[*] Fetching from remote"
  sudo -H -i -u "${BISQ_USER}" sh -c "cd ${BISQ_HOME}/${BISQ_REPO_NAME} && git fetch --prune --all"
}

# Takes 1 argument (BISQ_REPO_TAG_OR_BRANCH)
update-set-branch () {
  BISQ_REPO_TAG_OR_BRANCH=$1

  echo "[*] Checking out branch ${BISQ_REPO_TAG_OR_BRANCH}"
  sudo -H -i -u "${BISQ_USER}" sh -c "cd ${BISQ_HOME}/${BISQ_REPO_NAME} && git checkout ${BISQ_REPO_TAG_OR_BRANCH}"
}

update-pull-current-branch () {
  echo "[*] Pulling current branch"
  sudo -H -i -u "${BISQ_USER}" sh -c "cd ${BISQ_HOME}/${BISQ_REPO_NAME} && git pull --ff"
}

update-show-status () {
  echo "[*] Status for git repo at ${BISQ_HOME}/${BISQ_REPO_NAME}"

  echo "[*] Listing local branches"
  sudo -H -i -u "${BISQ_USER}" sh -c "cd ${BISQ_HOME}/${BISQ_REPO_NAME} && git branch -vv"

  echo "[*] Using remote"
  sudo -H -i -u "${BISQ_USER}" sh -c "cd ${BISQ_HOME}/${BISQ_REPO_NAME} && git remote -v"
}