# Bonded reputation

Bonded reputation converts a Bisq 1 `REPUTATION` bond into Bisq 2 reputation. The economic invariant
is that scored BSQ is either still locked or has been irreversibly destroyed; recoverable BSQ must not
remain available to back another profile after a canonical unlock.

## Bridge input

The Bisq 1 bridge is authoritative for the lockup amount, reputation hash, lock time, block height,
lockup transaction id and optional unlock transaction id. Oracle nodes must derive authorized data
only from that bridge output. Bisq 2 accepts only lockups with the protocol minimum lock time of
50,000 blocks.

An ordinary `LOCKUP` bridge entry has no unlock transaction id and contributes score. A canonical
`UNLOCK` entry identifies the same lockup and carries its unlock transaction id; it contributes zero
score and suppresses the corresponding lockup independently of message arrival order.

After the Bisq 1 hard-fork-3 activation, a non-canonical spend produces no `UNLOCK` entry but burns the
bond collateral. The score may remain until its authorized data expires, but the destroyed BSQ cannot
be recovered and reused for another profile. Pre-activation history must be audited separately before
assuming that invariant for historical data.

## Authorized data integrity

Authorized bonded-reputation data version `2` binds all security-relevant status to the oracle
signature, including:

- the data version;
- the lockup transaction id and block identity;
- the reputation hash, amount and lock time; and
- the presence and value of the unlock transaction id.

Changing a lockup into an unlock, stripping an unlock marker or relabeling current data as a future
version must invalidate the oracle signature and the enclosing authenticated-storage signature.

Version `1` excluded the unlock transaction id from both hashes for compatibility with clients which
predated that field. Consequently, an untrusted relay can change its presence without either signing
key. Version `1` remains parseable so legacy network data cannot crash an upgraded node, but it must
contribute zero score. Oracle nodes upgraded to version `2` rescan the bridge history and republish
current data before clients rely on bonded reputation.

## Consumer invariants

- Only the current authorized-data version contributes score.
- An unlock marker contributes zero and dominates the matching lockup regardless of arrival order.
- Separate lockups using the same reputation hash are scored independently; an unlock suppresses only
  its matching lockup transaction.
- Unknown future versions fail closed and contribute zero until their semantics are implemented.
