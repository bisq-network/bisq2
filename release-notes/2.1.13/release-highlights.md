# Bisq 2.1.13 Highlights

## Offer Validation

- Take-offer validation now confirms the offer belongs to the maker.

## Encoding and Reputation Compatibility

- Canonical protobuf encoding supports signed integer fields with ZigZag varints.
- Versioned witness validation preserves ownership bindings while migrating older persisted version-2 request data.

## Bridge and Bonded-Role Reliability

- Bridge recovery retains concurrent catch-up requests and identifies unsupported subscription RPCs.
- Bonded-role revalidation continues when individual persisted entries cannot be replayed.

## Platform Support and Diagnostics

- macOS updates select the installer for the current CPU architecture.
- Version information includes the Tor binary version as well as the Tor Browser version.
