# Unreadable store files

This specification defines what the node keeps when it cannot read a store file of the user completely, how long it
keeps it, and how it tells the user.

## Problem

A store skips a persisted entry which it cannot parse, and keeps the other entries (rule 4 of
[`../network/validation-at-parsing.md`](../network/validation-at-parsing.md)). The next write of the store deletes the
skipped entry. A store file which cannot be read at all is replaced by a backup, or by an empty store.

So a new parse rule of a later version, or a damaged file, can delete data which exists nowhere else, for example a
trade together with its trade chat, which holds the payment details. Only the rotating backups kept a copy. They are
pruned by size and age, nothing restores skipped entries from them, and the user was not told.

## Rules

1. **Scope.** The rules apply to the stores in the private and the settings directories (`db/private` and
   `db/settings`), except the stores of rule 9. Network and cache stores only log a skipped entry, as before.
2. **Trigger.** When a store loads and skips at least one entry which it cannot parse, the node keeps a copy of the
   store file before the store is written again. A skipped chat message within a trade channel counts as a skipped
   entry. When a store file cannot be read at all, the node moves the file into the archive and then restores the store
   from its backups, as before.
3. **Content.** The copy is the store file as it was read, byte for byte: the file at the store path, or the backup
   from which the node restored the store.
4. **Location and name.** The copies are in `db/unreadable`. The name of a copy is the file name of the store, the date
   and time of the copy in UTC (`yyyy-MM-dd_HHmmss`), and the first 16 hexadecimal characters of the SHA-256 hash of
   the content, joined with `_`. Example: `bisq_easy_trade_store.protobuf_2026-10-12_100000_3f1c9a0b7d2e4c58`.
5. **One copy per content.** The node does not keep a second copy with the same content for the same store, and it
   does not report such a copy again.
6. **Size limit.** All files in the folder together take at most 100 MB after a copy. If a new copy of a store with
   skipped entries would exceed this limit, the node keeps no copy. It does not delete existing copies for it. It logs
   an error and tells the user (rule 8). A file which cannot be read at all is moved without this limit. If the move
   fails, the node moves the file into the folder `corruptedFilesAtRead` next to the store, as before.
7. **Retention.** The node deletes a copy when it is older than 90 days, at the first start after that. It never
   deletes a copy earlier. At every start it also deletes the temporary files of a copy which was interrupted. It does
   not touch other files in the folder.
8. **Notification.** After the start, the desktop shows one overlay in the style of the restore dialog. It lists each
   affected store with its problem and its copy, says how long the copies are kept, and shows the folder of the copies,
   with a button which opens it. When the store of a file which cannot be read is restored from a backup, the overlay
   opens after the restore dialog closes. When the user has no profile, it opens before onboarding. Onboarding, or the
   notice of a new release, starts only after the overlay closes.
9. **Exceptions.** A store can choose another handling:
   - The stores of peers, of market prices and of chat notifications are handled like network stores, although they
     are in the settings directory: a skipped entry is only logged, and a file which cannot be read at all is moved
     into `corruptedFilesAtRead`, as before.
   - The store of API access keeps nothing: a skipped entry is only logged, and the node deletes a file which cannot be
     read at all.

## Reasons

- Rule 1: these stores hold the user's own data, for example identities, keys, trades, trade chats, mediation cases,
  payment accounts and settings. The node cannot get them back from the network. Network and cache stores are filled
  again from the network, and a notice about them would only confuse the user.
- Rule 2: the store deletes the skipped entries at its next write, so the copy has to be taken while the node reads
  the file. Moving a file which cannot be read keeps the earlier behavior, in which the store is restored from its
  backups.
- Rule 3: a copy of the whole file needs no code in each store. A later version, or a support tool, can read it with
  the parser of the store.
- Rule 5: a store which is not written during a session skips the same entries again at the next start. Without rule 5
  every start would add a copy and show the overlay again.
- Rule 6: the limit bounds the use of the disk by copies. A trade peer can put data into a trade which a rule of a later
  version rejects, and a damaged disk can produce many unreadable files. New copies are dropped instead of old ones, so
  that such data cannot push out an older copy before its retention ends. A move takes no further space on the disk, and
  a file which stays at the store path is overwritten when the store is restored from a backup. So the limit does not
  apply to a move, and a failed move falls back to the earlier folder.
- Rule 7: 90 days give the user and support time to recover a trade or the evidence of a dispute. The copies hold
  private keys and payment details, so they do not stay longer than necessary.
- Rule 8: the overlay lies in the same layer as the restore dialog and onboarding, so it cannot open behind them. Before
  onboarding the user learns that the old profile could not be read before creating a new one.
- Rule 9: the peers, the market prices and the chat notifications come from the network or are derived from chat
  messages, and a notice about them would only confuse the user. A store of API access written before client secrets
  were hashed holds plaintext secrets, and the store service deletes the backups so that the plaintext does not
  outlive its migration. A copy, or a file in `corruptedFilesAtRead`, would keep the plaintext. A client whose entry
  cannot be read has to pair again anyway.

## Privacy

The copies hold private data, for example keys, payment details and chats. The node creates them with the same file
permissions as the stores, readable only by the owner. It logs the file name of the store and the path of the copy, and
never the content. For a store file which it cannot read, it logs the type and the stack trace of the error, and the
message only of a file system error, which names a path. The backup function of the desktop copies the data directory,
so a user backup includes the copies.

## Compatibility

The rules change no persisted format and no message. Earlier versions do not read `db/unreadable`. The stores of
rule 1 write the folder `corruptedFilesAtRead` in their store directory only when a move into the archive fails. The
node does not delete files which earlier versions wrote there. Network and cache stores, and the stores with network
data of rule 9, still move a file which cannot be read into that folder, without a size limit and without retention,
as before. The store of API access deletes such a file now; earlier versions moved it into that folder.

## Limitations

- Values which the node skips because it cannot resolve an enum value (rule 8 of
  [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md)) do not count as skipped entries.
- A store which drops entries with its own code instead of the shared skip helpers does not trigger a copy. For example,
  the store of API access drops a client without a usable credential.
- Entries which are skipped while parsing on another thread do not count.
- Only the desktop shows the overlay. The Android node app reads the same list of unreadable store files, but needs its
  own user interface for it.
- The node does not restore a copy. A later version which can read the copy does not merge it into the store.
