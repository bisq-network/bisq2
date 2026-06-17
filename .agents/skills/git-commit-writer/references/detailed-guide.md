# Complete Guide to Writing Git Commit Messages

This comprehensive guide is based on [Chris Beams' definitive article](https://cbea.ms/git-commit/) on writing good commit messages. Reference this document for detailed explanations, examples, and the reasoning behind each rule.

## The Seven Rules of a Great Git Commit Message

### Rule 1: Separate subject from body with a blank line

Git uses the blank line between the subject and body as a delimiter. This allows Git tools to properly parse commits for different contexts.

**Why it matters:**
- `git log --oneline` shows only the subject line
- `git shortlog` groups commits by user with subject lines only
- Email-formatted patches use the subject as the email subject line
- GitHub's UI displays subjects and bodies separately

**Example:**
```
Fix authentication bug in user login

Users were unable to log in due to a validation error in the
password hashing function. The bcrypt comparison was using the
wrong salt parameter, causing all login attempts to fail.

This commit corrects the salt parameter and adds unit tests to
prevent regression.
```

Without the blank line, Git tools can't distinguish where the subject ends and the body begins.

### Rule 2: Limit the subject line to 50 characters

50 characters is not a hard limit, but a guideline to ensure readability.

**Why it matters:**
- Forces you to think concisely about the change
- GitHub warns beyond 50 characters
- GitHub truncates subject lines at 72 characters
- `git log --oneline` becomes difficult to scan with long subjects

**Tips:**
- If struggling to summarize, the commit may be doing too much (split into multiple commits)
- Sacrifice complete sentences for brevity when needed

**Examples:**

✅ Good (50 characters or less):
```
Add user authentication with JWT tokens
Fix memory leak in background sync process
Update dependencies to latest versions
```

❌ Too long:
```
Add comprehensive user authentication system with JWT tokens and refresh token rotation
```

### Rule 3: Capitalize the subject line

Simple consistency rule that improves readability.

**Examples:**

✅ Good:
```
Accelerate to 88 miles per hour
```

❌ Bad:
```
accelerate to 88 miles per hour
```

### Rule 4: Do not end the subject line with a period

Trailing punctuation wastes precious character space in the 50-character limit.

**Examples:**

✅ Good:
```
Open the pod bay doors
```

❌ Bad:
```
Open the pod bay doors.
```

### Rule 5: Use the imperative mood in the subject line

**Imperative mood** means "spoken or written as if giving a command or instruction."

**Examples of imperative mood:**
- Clean your room
- Close the door
- Refactor the authentication module

**Why imperative mood:**
- Git itself uses imperative mood when creating commits automatically
- It matches the convention Git established (e.g., "Merge branch 'feature'")
- It tells someone what applying the commit will do, not what you did

**The golden test:**
Your subject line should complete this sentence:
> If applied, this commit will **[your subject line]**

**Examples:**

✅ Good (imperative mood):
```
If applied, this commit will refactor authentication module
If applied, this commit will remove deprecated methods
If applied, this commit will fix bug in user login
If applied, this commit will update dependencies
```

❌ Bad (not imperative):
```
If applied, this commit will fixed bug in user login          (past tense)
If applied, this commit will fixes bug in user login          (present tense)
If applied, this commit will fixing bug in user login         (present participle)
If applied, this commit will I fixed the bug in user login    (first person)
```

**Correct forms:**

| ✅ Imperative | ❌ Other Forms |
|---------------|----------------|
| Refactor authentication for clarity | Refactored authentication, Refactors authentication, Refactoring authentication |
| Remove deprecated methods | Removed deprecated methods, Removes deprecated methods, Removing deprecated methods |
| Update getting started documentation | Updated getting started documentation, Updates documentation, Updating documentation |
| Merge pull request #123 from user/branch | Merged pull request, Merges pull request |

**Note:** Using imperative mood can feel awkward in writing because we don't normally speak that way. But it's perfect for Git commit subjects. Git itself uses it, so following the convention maintains consistency.

### Rule 6: Wrap the body at 72 characters

Git does not wrap text automatically. When you wrap text manually at 72 characters, Git can indent text while still keeping everything under 80 characters total.

**Why 72 characters:**
- Allows for Git's indentation while staying under 80 character terminals
- Improves readability in various contexts (emails, GitHub, terminal)
- Matches the standard Git convention

**How to wrap:**
- Most text editors can be configured to hard-wrap at 72 characters
- Use manual line breaks at logical points in your sentences

**Example:**
```
Fix authentication bug in user login

Users were unable to log in due to a validation error in the
password hashing function. The bcrypt comparison was using the
wrong salt parameter, causing all login attempts to fail.

This commit corrects the salt parameter and adds unit tests to
prevent regression. The tests verify both successful login
attempts and failed attempts with invalid credentials.
```

### Rule 7: Use the body to explain what and why vs. how

The code shows **how** a change was made. The commit message should explain:
- **What** was changed
- **Why** it was changed
- What was the problem being solved
- Why this solution was chosen over alternatives

**Why it matters:**
- Preserves institutional knowledge
- Helps future maintainers understand reasoning
- Provides context that code alone cannot convey
- Documents trade-offs and decisions

**Example comparing before/after:**

❌ Uninformative:
```
Fix bug
```

✅ Informative:
```
Fix authentication timeout in Safari

Safari has a stricter cookie security policy that was causing
session tokens to expire prematurely on the login page. The
previous implementation set cookies without the SameSite
attribute, which Safari treats as SameSite=Lax by default.

This commit explicitly sets SameSite=None with the Secure flag,
allowing the session token to persist correctly across the
authentication flow in Safari while maintaining security in
other browsers.

Fixes #456
```

**What to include in the body:**
- The problem or bug being addressed
- Why this approach was chosen
- Side effects or consequences of the change
- Links to relevant issues or documentation
- Anything that would help future developers understand the change

**What NOT to include:**
- How the code works (that's what the code itself shows)
- Changes that are obvious from the diff
- Implementation details that are self-evident

## Additional Best Practices

### Atomic Commits

Make commits that represent a single logical change. This makes:
- Commit messages easier to write (one clear purpose)
- Code reviews simpler (focused changes)
- Reverting easier if needed
- History more understandable

### Use the Command Line

While IDE integrations are convenient, they often limit access to Git's full power. Learning the command-line Git workflow provides:
- Better understanding of Git's concepts
- Access to advanced features
- Ability to write proper multi-line commit messages
- More control over staging and committing

### Reference Issues and PRs

When commits relate to issues or pull requests, reference them in the body:

```
Fix memory leak in background sync

The background sync process was accumulating WebSocket connections
without properly closing them. This caused memory to grow unbounded
over time, eventually leading to application crashes after several
hours of operation.

This commit ensures all WebSocket connections are properly closed
in a finally block, preventing the leak.

Fixes #789
Related to PR #790
```

### Be Consistent

If working on a team, establish conventions and follow them:
- Decide on subject line length limits
- Agree on when to use body text
- Standardize format for issue references
- Choose imperative mood consistently

### Learn Git Deeply

Reading [Pro Git](https://git-scm.com/book/en/v2) (available free online) provides deep understanding of Git's design philosophy and why these conventions matter.

## Common Anti-Patterns to Avoid

❌ **Vague subjects:**
```
Fix stuff
Update code
Changes
WIP
asdfasdf
```

❌ **Describing implementation instead of purpose:**
```
Change UserController.login() to use bcrypt.compare() with correct parameters
```

Better:
```
Fix authentication bug in user login

Corrected bcrypt salt parameter to fix validation errors preventing
users from logging in.
```

❌ **Including irrelevant information:**
```
Fix login bug (worked on this all day, finally figured it out!)
```

❌ **Not using imperative mood:**
```
Fixed login bug
Fixing login bug
This fixes the login bug
I fixed the login bug
```

## Examples of Great Commit Messages

### Example 1: Bug Fix
```
Fix race condition in cache invalidation

The previous implementation used separate read and write operations
for cache invalidation, creating a race condition where two threads
could both read stale data before either wrote the update. This
caused intermittent data inconsistencies in production.

This commit wraps the read-invalidate-write sequence in a lock,
ensuring atomic execution. Performance impact is minimal since
cache operations are infrequent.

Fixes #1234
```

### Example 2: Feature Addition
```
Add dark mode support to user interface

Implements a dark color theme that can be toggled in user
preferences. The theme is applied system-wide and persists
across sessions.

Color values follow WCAG 2.1 Level AA contrast requirements
for accessibility. The implementation uses CSS custom properties
for theme switching without page reload.

Closes #567
```

### Example 3: Refactoring
```
Extract authentication logic into separate service

The authentication code was scattered across multiple controllers,
making it difficult to maintain and test. This commit consolidates
all authentication logic into a dedicated AuthService.

Benefits:
- Single responsibility principle compliance
- Easier unit testing with dependency injection
- Consistent authentication behavior across endpoints
- Preparation for upcoming OAuth integration

No functional changes to authentication behavior.
```

### Example 4: Documentation
```
Document database migration process

Added comprehensive guide for running database migrations in
production environments. Includes rollback procedures and
troubleshooting steps for common issues.

This documentation was requested by DevOps team after the
incident last week where a migration was rolled back incorrectly.
```

## Summary

Great commit messages:
1. Have informative, concise subject lines (50 characters)
2. Use imperative mood ("Fix bug" not "Fixed bug")
3. Include blank line between subject and body
4. Wrap body text at 72 characters
5. Explain what and why, not how
6. Provide context and reasoning
7. Reference related issues and PRs

Poor commit messages cost time for future maintainers (including your future self). Good commit messages save time and preserve institutional knowledge.
