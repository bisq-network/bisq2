# Popup of a failed start

This specification defines what the desktop application shows when the start of its services fails, and how the popup
changes `bisq.conf` when the user picks a way out.

## Rules

1. A failed start shows one popup. The start of the services fails when one of its steps fails, or when it does not
   complete within the startup timeout.
2. The popup of a failed start shows the error and offers a bug report. Closing it shuts the application down. Rules 3
   to 5 define the exception: a failed start which a change of `bisq.conf` fixes.

### Failures which a change of bisq.conf fixes

3. Such a failure is no bug, so its popup offers no bug report. The specification of the failure defines which
   failures get this popup and which changes the popup offers.
4. The popup offers at most two choices, next to a button which only shuts down. Each choice appends its settings to
   `bisq.conf` (rules 6 to 8) and shuts down. Closing the popup, also with Escape or Enter, only shuts down.
5. The message of the popup says what each choice risks, so the buttons ask for no confirmation. It names the path of
   `bisq.conf` and says that removing the added lines undoes the choice. A program argument or a JVM option overrides
   `bisq.conf`, so the popup does not offer a choice whose setting the command line sets. It names that setting
   instead.

### Appending to bisq.conf

6. A choice appends one line `path=value` per setting to `bisq.conf`, instead of rewriting the file. A later line
   overrides an earlier value of the path. The comments and includes of the user stay.
7. The application writes the new content into a temporary file next to `bisq.conf`. It parses and checks that file
   with the same code as the start, in the directory of `bisq.conf`, so that a relative include resolves as at the
   start. It also checks that the file gives each setting its value. Only then does it move the file over `bisq.conf`. If `bisq.conf` links to another file, that file
   gets the lines and the link stays. If `bisq.conf` does not exist, the application creates it.
8. If the parse, the check or the write fails, `bisq.conf` stays as it was. The application then shows a warning with
   the path of `bisq.conf`, the reason and the lines which the user can add by hand. The warning offers no bug report,
   because the cause is usually the file of the user. Closing it shuts the application down.

## Reasons

- Rule 1: a second popup would cover the first one and hide the error. Before this rule, a failed start also showed
  the warning "Initialising applicationService failed." without any detail, on top of the popup with the error.
- Rule 3: a bug report does not help the user out of a failure which a setting fixes. The popup offers the setting
  instead, so the user does not have to find out which entry to add to which file.
- Rule 5: while the command line sets a setting, writing it into `bisq.conf` does not change the next start. The
  choice would shut the application down without an effect.
- Rule 6: a rewrite from the parsed content drops the comments of the user. Appending keeps the file as the user
  wrote it, and the user undoes the choice by removing the lines.
- Rules 7 and 8: the start ignores a `bisq.conf` which it cannot parse or which its check refuses, and with it every
  setting of the user. A failed or partial write must therefore not replace a working `bisq.conf`. A separate check
  for the append could accept a file which the start ignores; the popup would then report a choice which has no
  effect. The start currently refuses a `bisq.conf` with a substitution such as `${x}`, so the append refuses it too.
