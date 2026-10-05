# Store write results

A successful persistence result means that the serialized store replaced the current store file.
Serialization, temporary-file writes, and replacement failures must report failure to the caller.
An asynchronous write completes exceptionally on failure. A persistence client reports `false`
when its asynchronous write fails. Persisting all clients succeeds only if every client succeeds.

## Replacement of the store file

A write must not remove or move the current store file before the new file replaces it. The writer
first writes the complete new store to a temporary file. A backup copies the current file. Then one
rename replaces the current file with the temporary file, atomically where the file system supports
it. At every moment the store path therefore names either the previous or the new complete store.

Reason: the reader treats a missing store file as a new, empty store. If a write removed the current
file first, a failed rename or a stop of the process between the two steps would make the next start
begin with an empty store, although a backup exists. For the oracle this loses witness bindings.

## Failed writes

Domain services must not treat a failed write as a committed state transition. In particular, an
oracle must persist a witness binding before publishing the corresponding authorization. Otherwise
a restart could forget the binding and allow the same witness to authorize another profile.

Failure messages identify the store path and cause. They must not print the store contents, which
can include private keys and account data. The node logs a failed write where it happens, once, as
an error with the store path. For a file system error the log contains its message, which names a
path and the error of the operating system. For another cause, for example a failed serialization,
it contains only the type and the stack trace, because the message can contain values of the store.

Reason: a persistence client reports a failed asynchronous write only as the result `false`, without
the cause, and most callers do not read the result. Without the log entry, a node with a full disk
runs on with data which it does not save, and nothing shows why the data is missing after the next
start.

These rules do not change the persisted format. They describe completion of a write, not a
guarantee of durability against loss of power or storage hardware failure.
