# Data directory migrations

This specification defines how an app migrates its data directory when the user upgrades, what a migration must
guarantee, and what happens when a migration fails. It applies to all apps: desktop, API app, seed node, oracle node,
node monitor and the mobile node.

## Rules

1. **Version file.** The file `version` in the data directory holds the version of the app which last completed the
   migrations. A missing file, or a file which cannot be read or parsed, counts as version 2.1.1, the last version
   before migrations existed. Then all migrations run.
2. **When migrations run.** The app runs the migrations at every start, after the check that no other instance uses the
   data directory, and before it reads any store or creates any service. A migration can therefore change any file of
   the data directory, including store files and configuration.
3. **Which migrations run.** Each migration has the version of the first release which contains it. A migration runs if
   its version is above the stored version and not above the version of the app. The pending migrations run in the
   order of their versions.
4. **Versions of new migrations.** A new migration gets the version of the next release. A migration with the version
   of a published release never runs for users who already started that release.
5. **Idempotency.** A migration must reach the migrated state from every state which an earlier run or the app can
   leave:
   - a data directory which it already migrated (rules 1, 7 and 9),
   - a data directory which it migrated only partly, because the app stopped or crashed during the migration,
   - a new, empty data directory, as all migrations also run at the first start of a new installation,
   - data which the new version wrote, because the user started the app anyway after a failed migration (rule 8).
6. **Failures.** A migration reports a failure only if the app must not run without it, for example when it converts
   data which the new version needs in the new form. A step which the app can do without, such as removing old files,
   handles and logs its own errors. Any exception of a migration counts as a failure.
7. **Recording the version.** After all pending migrations succeeded, the app writes its version to the version file.
   The write is atomic: after a crash, the file holds either the old or the new content. If the write fails, the app
   logs it and starts. The migrations then run again at the next start.
8. **After a failed migration.** The app stops at the first failed migration. The later migrations do not run, and the
   version is not recorded. The app does not start, unless:
   - the option `application.startAfterFailedMigration` is `true`, or
   - the app can ask the user, and the user chooses to start anyway. The desktop app asks in its main window, and
     shutting down is the default choice. The other apps cannot ask.

   An app which does not start because of a failed migration exits with code 3. The systemd units of the seed node and
   the oracle node do not restart the app after this code.
9. **Next start.** After a failed migration, all migrations above the stored version run again at the next start, also
   those which succeeded.
10. **Progress.** While migrations run, the desktop app shows this in its main window. It shows nothing at a start
    without pending migrations.

## Reasons

- Rule 1: the version file exists only since migrations run. Treating an unreadable file like a missing one keeps the
  app startable with a damaged file. Rule 5 makes the resulting rerun of all migrations safe.
- Rule 2: a migration which changes store files must run before the stores are read, and it must not depend on the way
  each app starts its services. Earlier, the migrations ran after the stores were read, and only if each app called
  its base class, which no app did. So no release before this rule ran a migration.
- Rule 4: the app compares versions, not a list of migrations which already ran. A migration with an old version is
  below the stored version of every user who already started that version.
- Rule 5: the app can stop at any point, and rules 1, 7, 8 and 9 make migrations run again. Writing new files through
  a temporary file and a rename, and deleting old data last, helps a migration to meet this rule.
- Rule 8: starting on a data directory which is not fully migrated can make the app misbehave or damage data, so the
  safe choice is the default. The option exists so that a faulty migration does not keep a node down until a fixed
  release. While the option is set, the app also starts after later failed migrations without asking, so it should be
  removed after the start for which it was set. Automatic restarts would only run the failing migration again.
- Rule 10: a migration can take a while. Without feedback, the user would think that the app did not start.
