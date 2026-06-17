# Bisq Contribution Standards

Reference for C4 protocol compliance, GPG signatures, and commit message standards.

## C4 Protocol (Collective Code Construction Contract)

Bisq follows the C4 protocol for collaborative development.

### Core Principles

**1. Change-Oriented Development**
- Problems are identified as issues
- Solutions are proposed as patches (pull requests)
- Patches should solve exactly one identified problem

**2. Patch Requirements**
- [ ] Addresses a specific issue or adds stated value
- [ ] Follows project coding standards
- [ ] Includes tests where applicable
- [ ] Does not break existing tests
- [ ] Commit message explains the problem and solution

**3. Stakeholder Buy-In**

Contributors should consult with stakeholders before significant work:
- **Channels**: Keybase, GitHub discussions, mailing list
- **Why**: Ensures maintainer interest and community value
- **When**: Before starting work on major features or refactorings

**Validation**:
```markdown
## Stakeholder Buy-In Check

Evidence of discussion:
- [ ] GitHub issue: #{issue_number}
- [ ] Keybase discussion: {date, participants}
- [ ] Mailing list thread: {subject, date}
- [ ] Maintainer acknowledgment: {who, when}

If no evidence found:
- For major changes (>100 lines): ⚠️ Request stakeholder confirmation
- For minor changes (<100 lines): ✅ Acceptable to proceed
```

**4. Atomic Commits**

Each commit should represent a single logical change:
- ✅ "Fix null pointer in TradeManager.confirmTrade()"
- ✅ "Add validation for trade amounts"
- ❌ "Fix bugs and add features" (too broad)

**Validation**:
```bash
# Check if commit touches multiple unrelated concerns
gh api repos/{owner}/{repo}/pulls/{pr}/commits --jq '.[].files | length'

# If single commit changes >10 files in different modules, investigate atomicity
```

**5. Mergeable State**

PR must be ready for immediate merge:
- [ ] No WIP commits
- [ ] No placeholder code ("TODO: implement")
- [ ] No debugging/test code left in
- [ ] All requested changes addressed
- [ ] CI passing

## GPG Commit Signatures

All commits to Bisq must be GPG-signed for authenticity.

### Verification Process

```bash
# Check all commits in PR for GPG signatures
gh api repos/{owner}/{repo}/pulls/{pr}/commits \
  --jq '.[] | {
    sha: .sha,
    message: .commit.message | split("\n")[0],
    verified: .commit.verification.verified,
    reason: .commit.verification.reason
  }'
```

### Expected Output

**✅ All Signed**:
```json
{
  "sha": "abc123",
  "message": "Fix trade protocol state transition",
  "verified": true,
  "reason": "valid"
}
```

**❌ Unsigned Commit**:
```json
{
  "sha": "def456",
  "message": "Add fee validation",
  "verified": false,
  "reason": "unsigned"
}
```

### Verification Report Template

```markdown
## GPG Signature Verification

**Total Commits**: {count}
**Signed Commits**: {signed_count}
**Unsigned Commits**: {unsigned_count}

### Status: {✅ All Signed | ⚠️ Partially Signed | ❌ None Signed}

{if unsigned commits exist:}
**Unsigned Commits**:
- `{sha}`: {first_line_of_message} - Reason: {verification_reason}
- ...

**Action Required**:
1. Set up GPG signing: https://docs.bisq.network/contributor-checklist#1-set-up-gpg
2. Amend commits with signatures
3. Force push to update PR
```

## Git Commit Message Standards

Bisq follows Chris Beams' "7 Rules of a Great Git Commit Message".

### The Seven Rules

1. **Separate subject from body with blank line**
2. **Limit subject line to 50 characters**
3. **Capitalize the subject line**
4. **Do not end subject line with a period**
5. **Use imperative mood in subject line**
6. **Wrap body at 72 characters**
7. **Use body to explain what and why, not how**

### Integration with git-commit-writer Skill

The `git-commit-writer` skill provides detailed guidance on these rules. Reference it when commit messages need improvement:

```markdown
❌ **Poor Commit Message**:
```
fixed stuff
```

✅ **Suggested Improvement** (using git-commit-writer standards):
```
Fix memory leak in P2P message handler

The message handler was not releasing buffer memory after
processing peer messages, causing gradual memory growth
under high peer count conditions.

This commit adds proper cleanup in the finally block and
adds a unit test to verify memory is released.

Fixes #1234
```
```

### Validation Checklist

```bash
# Extract all commit messages from PR
gh api repos/{owner}/{repo}/pulls/{pr}/commits \
  --jq '.[].commit.message' > /tmp/commit_messages.txt
```

For each commit message, validate:

**Subject Line**:
- [ ] ≤ 50 characters
- [ ] Starts with capital letter
- [ ] No ending period
- [ ] Uses imperative mood ("Fix" not "Fixed", "Fixes", or "Fixing")

**Body** (if present):
- [ ] Blank line after subject
- [ ] Lines wrapped at 72 characters
- [ ] Explains "what" and "why", not "how"
- [ ] References related issues (Fixes #123, Closes #456)

**Common Issues**:

| Issue | Example | Fix |
|-------|---------|-----|
| Too long subject | "Fix the bug that was causing crashes when..." | Shorten to "Fix crash in trade confirmation dialog" |
| Wrong tense | "Fixed null pointer exception" | Change to "Fix null pointer exception" |
| Not capitalized | "fix trade protocol bug" | Change to "Fix trade protocol bug" |
| Has period | "Add validation logic." | Remove period: "Add validation logic" |
| Vague | "Update code" | Be specific: "Add overflow check to fee calculation" |

### Commit Message Quality Report

```markdown
## Commit Message Standards Review

**Total Commits**: {count}
**Compliant**: {compliant_count}
**Need Improvement**: {needs_improvement_count}

### Issues Found:

#### Commit {sha} - "{subject}"
- ❌ Subject too long (67 chars, limit 50)
- ✅ Capitalization correct
- ❌ Uses past tense ("Fixed" should be "Fix")
- ⚠️ No body explanation for non-trivial change

**Suggested Rewrite**:
```
Fix null pointer in TradeManager.confirmTrade()

The confirmTrade method did not check if the trade object
was null before accessing its properties, causing crashes
when called with invalid trade references.

This commit adds null check and throws IllegalArgumentException
with descriptive message for debugging.

Fixes #1234
```
```

### Edge Cases

**Merge Commits**:
- Auto-generated merge commit messages are acceptable
- Custom merge messages should follow same rules

**Revert Commits**:
- Git-generated revert messages are acceptable
- Should add explanation in body about why revert is needed

**Multi-Paragraph Bodies**:
- Acceptable for complex changes
- Each paragraph should focus on single aspect
- Maintain 72-character line wrap

**Issue References**:
- "Fixes #123" - closes issue when merged
- "Relates to #456" - references without closing
- "Part of #789" - for multi-PR features

## Contribution Standards Report Template

```markdown
## Bisq Contribution Standards Review for PR #{number}

### C4 Protocol Compliance

**Stakeholder Buy-In**:
- Evidence: {GitHub issue, Keybase, mailing list}
- Status: {✅ Confirmed | ⚠️ Recommended | ❌ Required}

**Atomic Commits**:
- Single logical changes: {✅|⚠️|❌}
- Issues found: {details}

**Mergeable State**:
- No WIP commits: {✅|❌}
- No placeholders: {✅|❌}
- CI passing: {✅|❌}

### GPG Signatures

**Status**: {✅ All Signed | ⚠️ {X} Unsigned | ❌ None Signed}

{if issues}
**Unsigned Commits**: {list with details}
**Action**: Setup GPG and amend commits

### Commit Messages

**Compliance**: {✅ All Good | ⚠️ {X} Need Improvement | ❌ Major Issues}

{if issues}
**Issues Found**:
- Commit {sha}: {specific issues}

**Recommendations**:
{specific rewrites using git-commit-writer patterns}

### Overall Contribution Standards Assessment

**Level**: {EXCELLENT | GOOD | NEEDS WORK | NON-COMPLIANT}
**Blockers**: {count} (must fix before merge)
**Recommendations**: {count} (should fix)
**Optional**: {count} (nice to have)
```

## Quick Reference

**Pre-PR Checklist** (for contributors):
- [ ] Discussed with stakeholders for major changes
- [ ] Each commit is atomic and well-described
- [ ] All commits GPG-signed
- [ ] Commit messages follow 7 rules
- [ ] No WIP or TODO code
- [ ] Tests pass
- [ ] Ready for immediate merge

**Reviewer Checklist**:
- [ ] Verify stakeholder buy-in for major changes
- [ ] Check commit atomicity
- [ ] Validate GPG signatures on all commits
- [ ] Review commit message quality
- [ ] Confirm no placeholders or WIP code
- [ ] Check CI status
