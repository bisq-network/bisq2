# Bisq 1 bridge block continuity

## Purpose

The Bisq 2 oracle derives proof-of-burn data, bonded-reputation lock/unlock state and bonded-role
revalidation triggers from the Bisq 1 DAO block bridge. The historical snapshot and live stream must
form a contiguous view. A transport interruption must not permanently omit a block transition.

## Snapshot boundary

The historical BSQ-block response carries the Bisq 1 DAO height at which its snapshot was taken. Every
returned block must be at or below that height. The response remains sparse and may omit blocks which
contain no exported transaction, so the highest returned block is not an adequate snapshot cursor.

The oracle subscribes before requesting a historical snapshot. The continuity-aware stream sends a
ready event after the bridge has registered the observer. The oracle performs a confirming snapshot
after that acknowledgement, so every block is included either by the registered stream or by the
snapshot. The ready event's height is not itself a completed snapshot cursor. Overlap is expected and
must be harmless.

## Cursor and overlap rules

- The catch-up cursor advances to a successfully completed snapshot height.
- A live block advances the cursor only when its height is exactly the next contiguous height.
- A live height above the expected next height triggers catch-up from the cursor rather than skipping
  the missing interval. Its transactions and block-completion callback are deferred until catch-up.
- A live block at or below the completed cursor is ignored, including for block-completion callbacks.
- Snapshot/live overlap is deduplicated by block height and transaction id before authorized data is
  queued. Deduplication entries are retained only while they can overlap an active historical request
  and are pruned when the completed cursor makes them unreachable by future recovery.
- Completing a recovered snapshot triggers bonded-role revalidation even if the sparse response
  contains no exported transactions.

Failed snapshots do not advance the cursor. Concurrent recovery triggers are coalesced so they cannot
create unbounded overlapping bulk requests.

## Stream termination

Both abnormal stream errors and normal stream completion are treated as loss of continuity. The
oracle resubscribes with bounded backoff and requests catch-up from the last contiguous height. A
subscription retry without catch-up is insufficient because the bridge streams only newly processed
blocks.

## Compatibility

The snapshot height is an additive protobuf response field. Older consumers ignore it. A response
from an older bridge has the default value zero; an upgraded oracle must not use zero to advance its
cursor. Coordinated deployments should upgrade the Bisq 1 bridge before relying on cursor-based
recovery.

The continuity-aware subscription RPC is additive, while the original stream remains available to
older consumers. An upgraded oracle requires the new RPC and therefore requires the compatible
bridge to be deployed first.
