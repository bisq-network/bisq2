---
name: bisq-pr-reviewer
description: Provide comprehensive Bisq PR review in Codex or Claude Code by integrating CodeRabbitAI feedback extraction with Bisq-specific security validation, contribution standards verification, and domain expertise for Bitcoin, P2P, trade protocol, DAO, Java, and JavaFX changes. Use when reviewing Bisq pull requests, validating contribution standards, extracting unresolved review comments, or performing security reviews of cryptocurrency code changes.
---

# Bisq PR Reviewer

## Purpose

Deliver comprehensive pull request review for Bisq repositories through a two-phase process: systematic CodeRabbitAI comment extraction followed by Bisq domain expertise analysis covering security validation, contribution standards compliance, and architecture patterns.

## Tooling

Use the host environment's normal file, shell, and GitHub tools. In Claude Code, the bundled `/review-pr` command can run the extraction workflow directly. In Codex, read `commands/review-pr.md` from this plugin and execute the same steps with `gh`, local file reads, and repository inspection.

## When to Use

Trigger this skill for:
- Comprehensive Bisq pull request reviews
- CodeRabbitAI feedback validation and extraction
- Security review of Bitcoin/crypto code changes
- P2P protocol or trade protocol modifications
- DAO governance impact assessment
- Contribution standards compliance verification

**Activation phrases**:
- "Review PR #123"
- "Check CodeRabbitAI feedback on bisq-network/bisq2#456"
- "Analyze pull request for Bisq standards"
- "Comprehensive security review of this PR"

## Two-Phase Review Workflow

### Phase 1: CodeRabbitAI Comment Extraction

Systematically extract and organize all review feedback. In Claude Code, execute the bundled `/review-pr` command:

```bash
SlashCommand: "/review-pr {pr_number or owner/repo#pr}"
```

In Codex, open `commands/review-pr.md` within this plugin and follow that command's phases manually.

**Phase 1 delivers**:
1. CI/CD status verification
2. Complete comment inventory (inline, review body, issue comments)
3. CodeRabbit nitpicks and duplicates extraction
4. Code-verified current status of each comment
5. Severity categorization (Critical, High, Medium, Nitpicks)
6. Numbered action item checklist with traceability

### Phase 2: Bisq Domain Analysis

After Phase 1 completion, apply Bisq-specific expertise across four domains:

#### 1. Security Validation

Identify security-sensitive files in the PR:

```bash
gh api repos/{owner}/{repo}/pulls/{pr}/files --jq '.[].filename' | \
  grep -E "(bitcoin|crypto|trade|wallet|key|transaction|p2p|dao)"
```

For each security-sensitive file, load and apply security checklist:

```bash
Read references/bisq-security-checklist.md
```

The security checklist covers:
- Private key handling validation
- Bitcoin transaction safety (fees, dust, overflow)
- Cryptographic operations approval
- P2P message validation
- Financial logic precision

Generate security assessment using template from `references/bisq-security-checklist.md`.

#### 2. Contribution Standards Compliance

Load contribution standards reference:

```bash
Read references/contribution-standards.md
```

**Verify GPG signatures**:
```bash
gh api repos/{owner}/{repo}/pulls/{pr}/commits \
  --jq '.[] | {sha: .sha, verified: .commit.verification.verified, reason: .commit.verification.reason}'
```

**Extract commit messages for validation**:
```bash
gh api repos/{owner}/{repo}/pulls/{pr}/commits --jq '.[].commit.message'
```

Apply validation for:
- C4 protocol compliance (stakeholder buy-in, atomic commits)
- GPG signatures on all commits
- Git commit message standards (via git-commit-writer skill integration)

For commit message improvements, reference git-commit-writer skill patterns.

#### 3. Architecture Patterns Compliance

Load architecture patterns reference:

```bash
Read references/bisq-architecture-patterns.md
```

Validate:
- Multi-module Gradle structure boundaries
- JavaFX MVVM pattern adherence
- Dependency injection (Guice) patterns
- Service layer separation
- Trade protocol state machine safety (if applicable)
- DAO governance logic correctness (if applicable)
- P2P protocol versioning and compatibility

#### 4. Domain-Specific Checks

**Trade Protocol Safety** (if trade-related changes):
```bash
Grep "TradeProtocol|TradingPeer|TradeManager" --output_mode files_with_matches
```

Validate state transitions, peer communication safety, and financial correctness.

**DAO Governance Impact** (if DAO changes):
```bash
Grep "DAO|Governance|Proposal|Vote|Bonding" --output_mode files_with_matches
```

Validate voting logic, proposal validation, and bond management.

## Complete Review Output

Generate comprehensive report combining both phases:

```markdown
# Bisq PR Review: #{pr_number}

## Phase 1: CodeRabbitAI Comment Analysis
[Output from /review-pr command]
- CI Status: {✅ Passing | ❌ {X} failures}
- Total Comments: {count}
- Critical Issues: {count}

## Phase 2: Bisq Domain Analysis

### 🔐 Security Review
[From bisq-security-checklist.md assessment]
**Risk Level**: {CRITICAL|HIGH|MEDIUM|LOW}
**Critical Findings**: {count}

### 📋 Contribution Standards
[From contribution-standards.md validation]
- GPG Signatures: {✅ All signed | ❌ {X} unsigned}
- Commit Messages: {✅ Compliant | ⚠️ {X} need improvement}
- C4 Protocol: {✅ Compliant | ⚠️ Issues found}

### 🏗️ Architecture Compliance
[From bisq-architecture-patterns.md validation]
- Module Structure: {✅ Correct | ⚠️ Concerns}
- Design Patterns: {✅ Followed | ⚠️ Violations}
- Domain Logic: {assessment}

### ✅ Combined Action Plan

#### Must Fix Before Merge
1. [Critical from CodeRabbit + Security + CI]
2. ...

#### Should Fix (High Priority)
1. [High priority from both phases]
2. ...

#### Nice to Have (Medium + Nitpicks)
[Combined list]

## Summary
- **Overall**: {READY TO MERGE | NEEDS WORK | MAJOR CONCERNS}
- **Blockers**: {count}
- **Security Risk**: {None|Low|Medium|High|Critical}
```

## Execution Steps

1. **Parse PR reference** from user input (format: "owner/repo#number" or just "number")
2. **Execute Phase 1**: Invoke `/review-pr` for CodeRabbitAI extraction
3. **Get PR file list**: `gh api repos/{owner}/{repo}/pulls/{pr}/files`
4. **Identify domains**: Detect security-sensitive, architecture, DAO, or trade protocol files
5. **Load relevant references**: Load only needed reference files for efficiency
6. **Apply domain validation**: Execute security, standards, and architecture checks
7. **Generate combined report**: Merge findings from both phases
8. **Prioritize actions**: Order by criticality (Security > CI > CodeRabbit > Standards > Architecture)

## Edge Cases

**Large PRs (>10 files)**:
- Prioritize security-sensitive files first
- Summarize architecture changes at module level
- Group related issues

**Security-Critical PRs**:
- Load full security checklist even for quick reviews
- Escalate critical findings immediately
- Recommend additional security testing

**First-Time Contributors**:
- Provide detailed standards explanation
- Link to Bisq contribution guidelines
- Offer GPG setup assistance if signatures missing

**DAO/Protocol Changes**:
- Apply extra scrutiny to governance and P2P code
- Simulate scenarios for validation
- Assess backward compatibility thoroughly

## Integration Points

**git-commit-writer skill**:
- Automatically reference when commit messages need improvement
- Use git-commit-writer patterns for suggested rewrites

**bisq-ai-contributor-tools ecosystem**:
- Complements planned `bisq-architecture` skill
- Could integrate with future `bitcoin-security` skill
- Works alongside `/review-pr` command

## Success Criteria

Complete review delivers:
- ✅ All CodeRabbitAI comments extracted and verified
- ✅ CI status documented
- ✅ Security validation for sensitive code
- ✅ GPG signature verification
- ✅ Commit message quality assessment
- ✅ Architecture compliance check
- ✅ Domain-specific safety validation
- ✅ Prioritized, actionable recommendations
- ✅ Clear merge decision with rationale

## Bundled Resources

**references/bisq-security-checklist.md**:
Comprehensive Bitcoin/crypto security patterns including private key handling, transaction safety, cryptographic operations, P2P validation, and financial logic checks.

**references/bisq-architecture-patterns.md**:
Bisq architecture validation covering Gradle modules, JavaFX MVVM, dependency injection, trade protocol state machines, DAO governance patterns, and P2P networking.

**references/contribution-standards.md**:
C4 protocol compliance, GPG signature verification, and git commit message standards with integration guidance for git-commit-writer skill.

Load references as needed based on PR content to maintain context efficiency.
