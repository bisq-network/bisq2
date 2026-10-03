# Reports to moderators

A user reports another user to the moderators with a `ReportToModeratorMessage`. The node of the user sends one
report to each moderator, encrypted for the moderator.

## Rules

1. A node accepts a received report only if the reporter profile ID of the report is the profile ID of the key which
   sent the encrypted message: the hex encoded hash of the sender's public key.
2. A node stores at most 10 reports of one reporter and at most 5 000 reports in total. It ignores a report above
   these limits. Reports which the moderator deletes do not count.
3. A report of a banned reporter is ignored, as before.
4. The reports which a node received before its services started are checked with the same rules.

## Reasons

- Rule 1: without it, anybody could send a report in the name of another user.
- Rule 2: every user can send reports to every node, and a node persists them. Without a limit, a peer could fill the
  disk and the memory of a node. A user profile costs a proof of work, so the limit per reporter limits each profile,
  and the total limit bounds the store.

## Compatibility

Unmodified nodes send a report with the key of the reporting profile, so their reports pass rule 1.

## Not covered

A node stores received reports also if none of its profiles is a moderator. A check of the moderator role would need
the bonded role data, which may not be available yet when the node processes reports which it received while it was
offline.
