# Requests to oracle nodes

This specification defines how an oracle node binds the requests of users to their sender. The rules of the single
request types are in [`registration.md`](registration.md) (bonded roles),
[`../reputation/account-age.md`](../reputation/account-age.md) and
[`../reputation/signed-witness.md`](../reputation/signed-witness.md). How a service receives the requests which the node
processed before the service started is defined in [`../network/startup-replay.md`](../network/startup-replay.md).

## Terms

- **Request**: a confidential message which a user sends to each oracle node. The request types are the profile
  timestamp request (`AuthorizeTimestampRequest`), the account timestamp requests (`AuthorizeAccountTimestampV1Request`,
  `AuthorizeAccountTimestampV2Request`), the account age request, the signed witness request and the bonded role
  registration.
- **Sender key**: the key which signed the confidential message. The oracle node gets it when it decrypts the message.

## Rules

### Sender of a request

1. The oracle node timestamps a profile only if the profile ID of the profile timestamp request is the hash of the
   sender key. It ignores other profile timestamp requests.
2. The oracle node binds the account age and signed witness requests to the sender in the same way (see
   [`../reputation/account-age.md`](../reputation/account-age.md#authorization-protocol)). For bonded role
   registrations, see [Sender of a registration](registration.md#sender-of-a-registration).
3. An account timestamp request names neither a profile nor a key of the sender, so the oracle node does not bind it
   to the sender key. The key of the payment account signs the request, and the hash of the account contains this key.
4. Rules 1 and 2 apply also to a request which the node processed before the service started, for example a request
   from the mailbox. The service receives such a request with its sender key when it starts.

## Reasons

- Rule 1: the profile ID is the hash of the key of the profile. Without rule 1, anybody could request the timestamp of
  the profile of another user, and the first request fixes the date which the oracle node keeps for a profile.
- Rule 1 does not limit the number of profiles. Anybody can create keys and request a timestamp for each of them (see
  [Open points](#open-points)).
- Rule 4: without the sender key, the service cannot check rules 1 and 2. It would have to accept such a request
  unchecked or ignore it.

## Compatibility

- Released clients (checked in v2.1.13) request the timestamp of the profile of a user identity with the key pair of
  this identity. The hash of its public key is the profile ID, so the oracle nodes accept their requests (rule 1).

## Verification

- A profile timestamp request of another key than the key of the profile is not timestamped, also when the node
  received it before the service started. The request of the owner of the profile is timestamped once, also when the
  node received it before the service started.

## Open points

- **Number of requests per sender.** Anybody can create new keys and send a profile timestamp request or an account
  timestamp request with each of them. The oracle node publishes a timestamp for each, so the requests can fill the
  stores of the timestamps of all nodes until they expire.
  - What would remove it: a cost per request, for example a proof of work, or a limit of new timestamps per sender and
    time.
  - Decision needed: the cost or the limit, and how it applies to the requests of released clients.
  - Who decides: the developers.
