# Configuration file bisq.conf

This specification defines how the applications read the configuration file `bisq.conf` in their data directory, and
what happens when they cannot read it.

## Rules

1. The settings of `bisq.conf` override the configuration of the application. Program arguments and JVM options
   override `bisq.conf`.
2. Without `bisq.conf` the application starts with its own configuration. A `bisq.conf` which is a link to a file which
   does not exist counts as a `bisq.conf` which cannot be read, not as a missing one.
3. The start fails if `bisq.conf` exists but cannot be read, cannot be parsed, or contains a substitution such as
   `${x}`. The application never starts without the settings of an existing `bisq.conf`. The error names the path of
   the file and the reason. The desktop application shows it in a dialog, the other applications log it and print it to
   the standard error. The application then exits with the failure status.
4. An include without `required()` may name a file which does not exist, as HOCON defines it. An include with
   `required()` of a missing file stops the start.
5. A change of `bisq.conf` by the application leaves a `bisq.conf` which cannot be read as it was and fails with an
   error. The application never replaces such a file with only its own settings.

## Reasons

- Rule 3: `bisq.conf` holds settings which protect the user, for example the Tor bridges, which hide from a censor that
  the user connects to Tor, the torrc override `ExcludeNodes`, or the switch of the Tor sandbox. Earlier versions
  logged an error and started without any setting of the file. The user then connected in a way which they did not
  choose and usually did not notice it, because the log was the only sign. A typing error, such as `ExcludeNodes = {ch}`
  without quotes, was enough.
- Rule 3: a file which cannot be opened, for example because of its permissions, would otherwise count as an empty
  file, because the parser treats a file which it cannot open like a missing one.
- Rule 5: earlier versions read such a file as empty when they wrote a setting, and so replaced the settings of the
  user with the new setting alone.
