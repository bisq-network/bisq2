# Contributing to Bisq 2

Bisq welcomes bug reports and feedback. Bisq has temporarily paused paid development for new contributors
and routine review of their pull requests. Maintainers may make exceptions for high-value contributions.
Before starting work, ask about priorities, review capacity, and available funding in the
[Development - Bisq v2](https://matrix.to/#/#bisq.v2.dev:bitcoin.kyoto) channel on Matrix.
Read the [announcement on contributions and compensation](https://bisq.network/blog/open-source-development-in-the-age-of-ai/).

Use [GitHub issues](https://github.com/bisq-network/bisq2/issues) for non-security bugs. Report any suspected
security issue privately using **Report a vulnerability** on the [Security Advisories page](https://github.com/bisq-network/bisq2/security/advisories).
Keep security reports and proposed fixes out of public issues, pull requests, and chat. See the [security policy](../../SECURITY.md).

Please also have a look at the [contributor checklist](https://bisq.wiki/Contributor_checklist).

## Communication Channels

Most communication about Bisq happens on [Matrix](https://bisq.chat).

Discussion about non-security code changes happens in GitHub issues and pull requests.

Discussion about ideas or new features can be posted on the
Github [discussions page](https://github.com/bisq-network/bisq2/discussions).

More specific proposals should be added to the [proposals](https://github.com/bisq-network/proposals/issues) repository.
See https://bisq.wiki/Proposals for details.

## Contributor Workflow

All Bisq contributors submit changes via pull requests. The workflow is as follows:

- Fork the repository
- Create a topic branch from the `main` branch
- [Sign off on and GPG-sign every commit](#sign-off-on-and-gpg-sign-every-commit)
- Squash redundant or unnecessary commits, preserving contributors' sign-offs and GPG-signing the resulting commits
- Submit a pull request from your topic branch back to the `main` branch of the main repository
- Make changes to the pull request if reviewers request them and __**request a re-review**__

Pull requests should be focused on a single change. Do not mix, for example, refactorings with a bug fix or
implementation of a new feature. This practice makes it easier for fellow contributors to review each pull request on
its merits and to give a clear ACK/NACK (see below).

## Reviewing Pull Requests

Bisq follows the review workflow established by the Bitcoin Core project. The following is adapted from
the [Bitcoin Core contributor documentation](https://github.com/bitcoin/bitcoin/blob/master/CONTRIBUTING.md#peer-review):

Anyone may participate in peer review which is expressed by comments in the pull request. Typically reviewers will
review the code for obvious errors, as well as test out the patch set and opine on the technical merits of the patch.
Project maintainers take into account the peer review when determining if there is consensus to merge a pull request (
remember that discussions may have been spread out over GitHub and Matrix). The following language is used within
pull-request comments:

- `ACK` means "I have tested the code and I agree it should be merged";
- `NACK` means "I disagree this should be merged", and must be accompanied by sound technical justification. NACKs
  without accompanying reasoning may be disregarded;
- `utACK` means "I have not tested the code, but I have reviewed it and it looks OK, I agree it can be merged";
- `Concept ACK` means "I agree in the general principle of this pull request";
- `Nit` refers to trivial, often non-blocking issues.

Please note that Pull Requests marked `NACK` and/or GitHub's `Change requested` are closed after 30 days if not
addressed.

## Compensation

Bisq is not a company, but operates as a _decentralized autonomous organization_ ([DAO](https://bisq.network/dao/)).

Development teams work within budgets. If you expect payment, confirm funding with the relevant maintainers before
starting. For agreed, budgeted work, you can [request compensation in BSQ](https://bisq.wiki/Making_a_compensation_request),
the Bisq DAO's native token. The DAO decides whether to approve each request. A merged pull request does not guarantee payment.

## Style and Coding Conventions

### Configure Git user name and email metadata

See https://help.github.com/articles/setting-your-username-in-git/ for instructions.

### Write well-formed commit messages

From https://chris.beams.io/posts/git-commit/#seven-rules:

1. Separate subject from body with a blank line
2. Limit the subject line to 50 characters (*)
3. Capitalize the subject line
4. Do not end the subject line with a period
5. Use the imperative mood in the subject line
6. Wrap the body at 72 characters (*)
7. Use the body to explain what and why vs. how

*) See [here](https://stackoverflow.com/a/45563628/8340320) for how to enforce these two checks in IntelliJ IDEA.

### Sign off on and GPG-sign every commit

Each commit you submit must include a `Signed-off-by` trailer and a GPG signature. Use `git commit -s -S` to add both.

- `-s` adds `Signed-off-by: Your Name <you@example.org>` to the commit message using your Git committer name and email. By signing off, you certify that your contribution meets the terms of the [Developer Certificate of Origin 1.1](../../DCO).
- `-S` signs the commit with your GPG key. See [GitHub's instructions for signing commits](https://docs.github.com/en/authentication/managing-commit-signature-verification/signing-commits).

If your latest local commit is missing either, run `git commit --amend --no-edit -s -S`.

### Use an editor that supports Editorconfig

The [.editorconfig](.editorconfig) settings in this repository ensure consistent management of whitespace, line endings
and more. Most modern editors support it natively or with plugin. See http://editorconfig.org for details. See
also [bisq-network/style#10](https://github.com/bisq-network/style/issues/10).

### Keep the git history clean

It's very important to keep the git history clear, light and easily browsable. This means contributors must make sure
their pull requests include only meaningful commits (if they are redundant or were added after a review, they should be
removed) and _no merge commits_.
