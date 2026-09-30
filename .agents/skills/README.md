# Bisq AI Skills

Repo-local skills in this directory are loaded automatically by AI coding agents when working in the Bisq 2 checkout.

- Codex scans `.agents/skills` from the current directory up to the Git root.
- Claude Code uses `.claude/skills` for repository skills.

## Available Skills

### bisq-contributor-workflow

Guides day-to-day Bisq implementation work: issue analysis, narrow changes, Java/JavaFX conventions, Gradle verification, P2P safety, wallet and transaction checks, DAO impact, and documentation updates.

### bisq2-javafx-ui

Guides production-ready Bisq2 JavaFX UI work with strict MVC structure, lifecycle cleanup, design system conventions, navigation wiring, automation selectors, desktop harness verification, and review checklists.

### bisq-pr-reviewer

Performs comprehensive Bisq pull request review by combining review-comment extraction with contribution standards, architecture review, and Bitcoin/P2P security validation.

### git-commit-writer

Drafts and reviews professional commit messages with imperative subjects, concise summaries, and body text focused on what changed and why.

### ui-design-principles

Applies pragmatic UI quality checks for JavaFX, web, desktop, mobile, and CLI interfaces.

## Required Structure

```text
{agent-root}/skills/{skill-name}/
+-- SKILL.md
+-- agents/openai.yaml
+-- references/          # Optional, for detailed material loaded as needed
```

Use `references/` for long checklists or examples. Keep the main `SKILL.md` focused on workflow and routing.

## Sanity Check

Run from the repository root after editing skills:

```bash
find .agents/skills .claude/skills -maxdepth 2 -name SKILL.md | sort
diff -qr .agents/skills .claude/skills
git diff --check -- .agents/skills .claude/skills
```
