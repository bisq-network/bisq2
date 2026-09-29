# Agent Instructions and Skills

This directory is the source of the instructions for AI coding agents.

| Path | Content |
|---|---|
| `instructions/[0-9]*.md` | Always-on instructions for all agents, in file-name order |
| `instructions/claude/[0-9]*.md` | Optional: only for Claude Code |
| `instructions/codex/[0-9]*.md` | Optional: only for Codex and other agents that read `AGENTS.md` |
| `skills/<name>/SKILL.md` | Skills: procedures that an agent loads only when their trigger applies |
| `build-instructions.sh` | Generates `AGENTS.md`, `CLAUDE.md`, and `.aiassistant/rules/bisq.md` |

## Editing

1. Edit or add files in `instructions/` or `skills/`. Never edit the generated files.
2. Run `.agents/build-instructions.sh`.
3. Commit the source files and the generated files together.

`.agents/build-instructions.sh --check` fails if a generated file is out of date.

## How the agents load the files

* Codex and most other agents read `AGENTS.md`. It has no import syntax, so it is a concatenation of all sources.
* Codex reads at most 32 KiB of project instructions by default and silently drops the rest. `AGENTS.md` is larger, so `.codex/config.toml` raises the limit to 64 KiB. Codex applies this project setting only when the user has marked the project as trusted. Without it, Codex sees only the beginning of `AGENTS.md`. For this reason the project overview and the skill list come first. `build-instructions.sh` fails if `AGENTS.md` exceeds 64 KiB.
* Claude Code reads `CLAUDE.md`. It contains `@` imports of the source files, which Claude Code expands at startup. Claude Code warns about instruction files longer than 200 lines; small imported files avoid that warning.
* JetBrains AI Assistant reads `.aiassistant/rules/bisq.md`, which has the same content as `AGENTS.md`.
* Codex reads skills from `.agents/skills/`. Claude Code reads them from `.claude/skills`, which is a symbolic link to `.agents/skills`.

## Rules for the content

* State each rule once. A rule for all agents belongs in `instructions/`, not in a tool-specific directory.
* Keep always-on behavior in `instructions/`. Only a procedure with a clear trigger belongs in a skill.
* List every skill with its trigger in `instructions/05-agent-skills.md`. The skill `description` decides whether an agent loads the skill automatically, so state the trigger concretely.
* Personal preferences of one developer belong in that developer's global agent configuration, not here.
