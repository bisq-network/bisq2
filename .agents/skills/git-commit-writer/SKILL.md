---
name: git-commit-writer
description: Guide Codex or Claude Code to write professional, informative Git commit messages following industry best practices. Use this skill when writing commit messages, reviewing proposed commits, helping users improve commit message quality, or handling git, version control, or contribution documentation tasks.
---

# Git Commit Writer

## Overview

This skill enables writing professional Git commit messages that follow industry best practices based on Chris Beams' widely-adopted guidelines. Well-crafted commit messages preserve institutional knowledge, improve code maintainability, and make collaboration more effective in either Codex or Claude Code.

## When to Use This Skill

Activate this skill when:
- Writing a new commit message for code changes
- Reviewing or improving an existing commit message
- User asks "how should I write this commit message?"
- User provides a poor commit message and needs help improving it
- User mentions git commit, version control, or commit standards
- User is about to commit code and needs guidance

## The Seven Core Rules

Apply these rules to every commit message:

### 1. Separate subject from body with a blank line

Place one blank line between the subject line and the body text. Git tools rely on this separation.

```
Fix authentication bug in user login

The bcrypt comparison was using the wrong salt parameter.
This commit corrects the parameter and adds unit tests.
```

### 2. Limit the subject line to 50 characters

Keep subjects concise and scannable. GitHub warns beyond 50 characters and truncates at 72.

✅ Good: `Add user authentication with JWT tokens`
❌ Too long: `Add comprehensive user authentication system with JWT tokens and refresh`

### 3. Capitalize the subject line

Start with a capital letter for consistency.

✅ Good: `Refactor authentication module`
❌ Bad: `refactor authentication module`

### 4. Do not end the subject line with a period

Periods waste space in the 50-character limit.

✅ Good: `Fix memory leak in sync process`
❌ Bad: `Fix memory leak in sync process.`

### 5. Use the imperative mood in the subject line

Write commands, not descriptions of what was done.

**The golden test:** Complete this sentence:
"If applied, this commit will **[your subject line]**"

✅ Imperative: `Fix bug in user login`
✅ Test: "If applied, this commit will **fix bug in user login**"

❌ Past tense: `Fixed bug in user login`
❌ Test: "If applied, this commit will **fixed bug in user login**" (grammatically incorrect)

**Common imperative forms:**
- Fix, Add, Remove, Update, Refactor, Improve
- Merge, Revert, Document, Extract, Consolidate

**Avoid:**
- Fixed, Added, Removed (past tense)
- Fixes, Adds, Removes (present tense)
- Fixing, Adding, Removing (present participle)
- I fixed, I added (first person)

### 6. Wrap the body at 72 characters

Hard-wrap body text at 72 characters for proper display in terminals and Git tools.

### 7. Use the body to explain what and why vs. how

The code shows **how** something was done. The commit message explains:
- **What** was changed
- **Why** it was changed
- What problem was being solved
- Why this solution was chosen

## Workflow for Writing Commit Messages

### Step 1: Understand the Changes

Before writing the commit message, review what was changed:
- What files were modified?
- What functionality was added, removed, or fixed?
- Is this a single logical change (atomic commit)?

### Step 2: Craft the Subject Line

Write a subject line that:
1. Completes: "If applied, this commit will **[subject]**"
2. Uses imperative mood (Fix, Add, Remove, Update, etc.)
3. Stays under 50 characters
4. Starts with a capital letter
5. Has no ending period

**Subject line patterns:**

| Pattern | Example |
|---------|---------|
| Fix [issue] in [area] | `Fix race condition in cache invalidation` |
| Add [feature] to [area] | `Add dark mode support to user interface` |
| Remove [thing] from [area] | `Remove deprecated authentication methods` |
| Update [thing] to [state] | `Update dependencies to latest versions` |
| Refactor [area] for [reason] | `Refactor auth logic for maintainability` |
| Improve [aspect] of [area] | `Improve error messages in validation` |

### Step 3: Decide If a Body Is Needed

**Body is required when:**
- The change is not self-explanatory from the subject and diff
- There's important context or reasoning to preserve
- The change fixes a bug with non-obvious root cause
- Trade-offs or alternatives were considered
- The change has side effects or consequences

**Body is optional for:**
- Simple, obvious changes
- Typo fixes (though consider if body would help)
- Very small refactorings with clear purpose

### Step 4: Write the Body (If Needed)

Structure the body to answer:

1. **What was the problem or motivation?**
   - What bug occurred or what was missing?
   - What user need is being addressed?

2. **Why this solution?**
   - Why was this approach chosen?
   - What alternatives were considered?

3. **What are the effects?**
   - Are there side effects or consequences?
   - Does this enable future changes?

4. **References**
   - Link to related issues: `Fixes #123`
   - Link to PRs: `Related to PR #456`
   - Link to documentation or discussions

**Example template:**
```
[Subject line]

[What was the problem - 1-2 sentences]

[Why this solution - 1-3 sentences explaining the approach]

[Optional: Effects, consequences, or next steps]

Fixes #123
```

### Step 5: Apply Final Checks

Before committing, verify:
- [ ] Subject line is 50 characters or less
- [ ] Subject uses imperative mood
- [ ] Subject is capitalized with no ending period
- [ ] Blank line separates subject from body
- [ ] Body is wrapped at 72 characters
- [ ] Body explains what and why, not how
- [ ] Related issues are referenced

## Common Scenarios and Examples

### Scenario 1: Simple Bug Fix

**User request:** "I fixed a login bug, what should my commit say?"

**Response pattern:**
```
Subject: Fix authentication bug in user login

Body (optional):
The password validation was failing due to incorrect bcrypt
salt parameter. This commit corrects the parameter and adds
unit tests to prevent regression.

Fixes #789
```

### Scenario 2: Feature Addition

**User request:** "I just added dark mode support"

**Response pattern:**
```
Subject: Add dark mode support to user interface

Body:
Implements a dark color theme toggleable in user preferences.
The theme persists across sessions and follows WCAG 2.1
Level AA contrast requirements for accessibility.

Uses CSS custom properties for instant theme switching without
page reload.

Closes #567
```

### Scenario 3: Refactoring

**User request:** "I cleaned up the authentication code"

**Response pattern:**
```
Subject: Extract authentication logic into separate service

Body:
Authentication code was scattered across multiple controllers,
making it difficult to maintain and test. This commit
consolidates all authentication logic into a dedicated
AuthService.

Benefits:
- Single responsibility principle compliance
- Easier unit testing with dependency injection
- Consistent authentication behavior across endpoints

No functional changes to authentication behavior.
```

### Scenario 4: Reviewing a Poor Commit

**User provides:** `"Fixed stuff"`

**Response pattern:**
Suggest improvement:
```
The commit message "Fixed stuff" is too vague. Consider a more
descriptive subject like:

Fix memory leak in background sync process

And add body text explaining:
- What was the memory leak?
- What was causing it?
- How does this fix address it?
```

## Handling Edge Cases

### Multiple Related Changes

If multiple related changes are in one commit:
- List them in the body with bullet points
- Ensure they're logically related (otherwise split commits)

```
Update authentication system for security

- Add rate limiting to login endpoint
- Implement JWT token refresh mechanism
- Update bcrypt rounds to current best practice

These changes work together to improve authentication security
after the security audit recommendations.
```

### Merge Commits

Git creates these automatically, typically don't need custom messages. If customizing:

```
Merge branch 'feature/dark-mode' into main

Brings dark mode support with accessibility compliance.
All tests passing and code reviewed.
```

### Reverting Commits

Use `git revert` which auto-generates messages, but enhance with context:

```
Revert "Add experimental caching layer"

This reverts commit abc123def456.

The caching layer caused data inconsistencies in production
under high concurrent load. Reverting while we investigate
the race condition issue.

Related to incident #2345
```

## Resources

This skill includes:

### references/detailed-guide.md

Comprehensive guide with:
- Detailed explanations of all seven rules
- Extensive examples and anti-patterns
- Common scenarios and edge cases
- Links to authoritative sources

Load this reference when:
- User asks for detailed explanations
- User needs examples of specific patterns
- Helping with complex commit scenarios
- User wants to understand the "why" behind rules

## Project-Specific Considerations

When working in specific projects:
- Check for existing commit message conventions in CONTRIBUTING.md or similar files
- Follow established patterns in the project's git history
- Note any project-specific prefixes (e.g., `feat:`, `fix:`, `docs:`)
- Respect team decisions that differ from these guidelines (consistency matters more than personal preference)

## Anti-Patterns to Avoid

Never accept these commit message patterns:
- ❌ Vague: `Fix stuff`, `Update code`, `Changes`, `WIP`
- ❌ Non-imperative: `Fixed bug`, `Updating code`, `I added feature`
- ❌ Too long subjects: Anything over 72 characters
- ❌ Implementation details in subject: `Change function X to use Y instead of Z`
- ❌ Missing context: `Fix bug` (which bug? what was broken?)
- ❌ Casual language: `Finally got this working!`, `asdfasdf`

## Success Criteria

A well-written commit message should:
1. Allow future developers to understand the change without reading the code
2. Explain the reasoning and context behind decisions
3. Enable quick scanning of git history
4. Preserve institutional knowledge
5. Make code reviews and debugging easier
