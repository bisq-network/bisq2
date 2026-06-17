---
name: bisq-contributor-workflow
description: Guide Codex or Claude Code through high-quality Bisq contribution work. Use when implementing Bisq issues, fixing bugs, changing Java or JavaFX code, touching P2P networking, Bitcoin wallet or transaction logic, DAO governance, build/test workflows, documentation, or contributor onboarding tasks in Bisq repositories.
---

# Bisq Contributor Workflow

## Goal

Help Bisq contributors make small, well-verified changes that respect Bisq's decentralized, security-sensitive architecture. Prefer repository-local patterns over generic framework advice.

## Core Workflow

1. Read the issue, nearby code, tests, and project docs before editing.
2. Identify the affected domain: desktop UI, trade protocol, P2P networking, wallet/Bitcoin, DAO, persistence, build tooling, or docs.
3. Keep changes narrow. Avoid broad refactors unless they directly reduce risk for the requested change.
4. Run the smallest meaningful verification first, then broaden when touching shared or security-sensitive code.
5. Summarize behavioral impact, tests run, and residual risk.

## Bisq-Specific Checks

- For Bitcoin, wallet, key, fee, address, or transaction code, verify private-key handling, amount precision, dust/fee boundaries, and network selection.
- For P2P or trade protocol changes, check message validation, state transitions, replay/idempotency behavior, and peer trust assumptions.
- For JavaFX UI changes, preserve the existing MVC structure, view/controller boundaries, resource usage, and responsive layout behavior.
- For DAO governance changes, verify proposal/vote lifecycle assumptions and backwards compatibility with existing DAO state.
- For docs and prompts, prefer contributor workflow clarity over marketing copy.

## Verification

Use commands from the target Bisq repository rather than inventing new ones. Common patterns include:

```bash
./gradlew test
./gradlew :desktop:desktop-app:test
./gradlew :core:test
```

If a module or task name differs, inspect `settings.gradle`, `build.gradle`, or `./gradlew tasks` and choose the nearest focused command. When tests are too expensive or unavailable, state exactly what was inspected instead.

## Related Resources

For review-heavy work, use `bisq-pr-reviewer`. For UI-specific polish, use `ui-design-principles`. For commit messages, use `git-commit-writer`.
