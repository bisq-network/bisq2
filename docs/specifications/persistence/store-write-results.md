# Store write results

A successful persistence result means that the serialized store replaced the current store file.
Serialization, temporary-file writes, and replacement failures must report failure to the caller.
An asynchronous write completes exceptionally on failure. A persistence client reports `false`
when its asynchronous write fails. Persisting all clients succeeds only if every client succeeds.

Domain services must not treat a failed write as a committed state transition. In particular, an
oracle must persist a witness binding before publishing the corresponding authorization. Otherwise
a restart could forget the binding and allow the same witness to authorize another profile.

Failure messages identify the store path and cause. They must not print the store contents, which
can include private keys and account data.

These rules do not change the persisted format. They describe completion of a write, not a
guarantee of durability against loss of power or storage hardware failure.
