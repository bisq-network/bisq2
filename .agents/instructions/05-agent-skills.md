## Agent Instructions
`AGENTS.md`, `CLAUDE.md`, and `.aiassistant/rules/bisq.md` are generated from the files in `.agents/instructions/`.
Do not edit the generated files. Edit the source files and run `.agents/build-instructions.sh`.
See `.agents/README.md`.

## Agent Skills
Task-specific instructions for AI agents are in `.agents/skills/<name>/SKILL.md`. `.claude/skills` is a
symbolic link to `.agents/skills`, so that Claude Code finds the same skills. On Windows, the link needs
`core.symlinks=true` (with Developer Mode); otherwise Git checks out a text file instead of the link.

Do not read skills by default. Agents that load skills automatically select them by their description.
Other agents read a skill's `SKILL.md` only when the task touches the area listed here:
- `bisq-contributor-workflow`: implementation tasks and contribution flow
- `bisq2-javafx-ui`: JavaFX desktop UI changes
- `bisq-pr-reviewer`: Bisq pull request review
- `git-commit-writer`: commit message drafting
- `ui-design-principles`: UI quality review
- `canonical-encoding`: canonical encoding (`bisq.common.encoding.canonical`) of hash and signature
  preimages: classes that implement `Canonical` or define a `CanonicalSchema`, their `@ExcludeForHash`
  fields, and `serializeForHash()` in hash, signature, or proof of work paths
- `root-cause-analysis`: non-trivial bugs, unexpected behavior, domain inconsistencies, security-sensitive
  changes, and problems with several edge cases. Load it before proposing a fix or presenting alternatives
- `write-specification`: creating or updating a specification in `docs/specifications/` (see **Specifications**)
- `review-feedback`: the user provides feedback prefixed with `reviewer claims:`
- `release-notes`: release notes are requested
