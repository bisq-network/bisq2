# Reports to moderators

A user reports another user to the moderators with a `ReportToModeratorMessage`. The node of the user sends one
report to each moderator, encrypted for the moderator.

## Rules

1. A node accepts a received report only if the reporter profile ID of the report is the profile ID of the key which
   sent the encrypted message: the hex encoded hash of the sender's public key.
2. A node stores at most 10 reports of one reporter. It ignores a further report of that reporter. Reports which the
   moderator deletes do not count.
3. A node stores at most 5 000 reports in total. When a report arrives while the node stores 5 000 reports, the node
   drops one report from the stored reports and the new report, in this order:
   1. a report of a reporter whose user profile the node does not know;
   2. among these, or if there is none, a report of the reporter with the most stored reports;
   3. among these, the report with the oldest date.

   If the dropped report is the new report, the node ignores it. Otherwise it stores the new report. A report which
   is already stored is ignored and drops no other report.
4. The node decides about a report and changes the stored reports in one step, so two reports which arrive at the
   same time cannot exceed the limits.
5. The reports which the moderator's node creates itself for users who exceed the message rate limit are stored
   with rules 3 and 4. Rule 2 does not apply to them, because they all have the moderator as reporter.
6. A report of a banned reporter is ignored, as before.
7. The reports which a node received before its services started are checked with the same rules.

## Reasons

- Rule 1: without it, anybody could send a report in the name of another user.
- Rules 2 and 3: every user can send reports to every node, and a node persists them. Without a limit, a peer could
  fill the disk and the memory of a node.
- Rule 3: the limit per reporter does not bound a sender who uses many keys. The reporter of a report is the key which
  sent it (rule 1); a new key costs nothing, and each report costs only the proof of work of one message. If a full
  store ignored every new report, such a sender could fill it and switch off reporting until the moderator deletes
  reports. So a full store drops a report instead. Honest reporters have a published user profile, which the node of
  the moderator normally knows, so the reports of unknown reporters are dropped first. A sender who floods with
  reports of known profiles drops mostly the own reports, because that sender has the most.
- Rule 5: a peer can trigger such a report with every profile which exceeds the rate limit. Without rule 5 these
  reports would grow the store without a limit. All of them have one reporter, so a full store drops them first; a
  report of a user counts more than a report which the node created itself.

## Compatibility

Unmodified nodes send a report with the key of the reporting profile, so their reports pass rule 1.

## Limitations

- A sender who publishes many user profiles and sends one report from each can push out old reports of known
  reporters, because these reporters then have as many reports as each profile of the sender. A user profile costs a
  proof of work of about 2^16 hashes and the proof of work of its publication.
- The sender chooses the date of a report, within the limits of the network (at most 2 hours in the future). Among
  reports of equal rank, a sender who dates its reports in the future keeps them, and older reports are dropped
  first. The node keeps no time of arrival for a report, which would remove this.
- A report of an honest reporter whose profile the node does not know is dropped before the reports of known
  reporters: if the profile has not reached the node yet, or if it expired because its owner did not publish it again.
- A dropped report is gone for this session. It can arrive again only if the mailbox delivers it again, for example
  after a restart.

## Not covered

A node stores received reports also if none of its profiles is a moderator. A check of the moderator role would need
the bonded role data, which may not be available yet when the node processes reports which it received while it was
offline.
