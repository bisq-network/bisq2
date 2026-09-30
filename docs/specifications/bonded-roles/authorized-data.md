# Authorization of authorized data

This specification defines which key may authorize authorized data, and where a node checks it. Which key may
publish and remove the data in the network storage is defined in
[`../network/storage-ownership.md`](../network/storage-ownership.md). How a bonded role is registered is defined in
[`registration.md`](registration.md).

## Terms

- **Authorized data**: data which only the holder of a certain role may create, for example a bonded role, an alert,
  a ban of a user profile, or reputation data. The network storage holds it as `AuthorizedData`: the data, the
  authorized public key, and the authorization signature of that key over the data.
- **Static keys**: public keys which are written in the code, one set per role type (oracle node, security manager,
  release manager, moderator). In dev mode, one set of dev keys replaces all sets.
- **Static flag**: the field `staticPublicKeysProvided` of the data. It states that a static key authorized the data.
- **Authorizing role type**: the type of bonded role which may authorize a type of data. For example, an oracle node
  authorizes bonded roles and reputation data, and a security manager authorizes alerts.

## Rules

1. The authorization signature must be valid for the authorized public key and the data.
2. If the data has the static flag, the authorized public key must be one of the static keys of the type of the data.
3. If the data does not have the static flag, the authorized public key must be the authorized key of a bonded role of
   the authorizing role type. A banned role does not count, unless the node is configured to ignore the security
   manager.

### Where a node checks the rules

4. The network storage checks rules 1 and 2 when it adds the data. It does not know the bonded roles, so it does not
   check rule 3.
5. The network storage does not check stored data again when it loads a store.
6. The services which use authorized data check rule 2 or rule 3 before they use the data
   (`AuthorizedBondedRolesService.hasAuthorizedPubKey`). For rule 2 the check compares the authorized public key with
   the static keys of the version of the node. It does not rely on the check of the network storage.
7. If no bonded role matches (rule 3), the application keeps the data and checks it again when a bonded role or an
   oracle node is added. Bonded roles can arrive after the data which they authorize.

## Reasons

- Rule 6: a later version can remove a static key, for example after the key was compromised. A node which updates
  still holds the data which that key published, for up to 100 days, and the store does not check it again (rule 5).
  If the application relied on the check of rule 4, it would still use this data. The root role of an oracle node has
  the static flag. With the root role of a removed key, that key could authorize bonded roles of every type on this
  node, because rule 3 would find its role.
- Rule 6 needs no signature verification. It is a lookup in a set of a few keys.
- Rule 5: checking the authorization of every stored entry verifies one signature per entry. This check was disabled
  at startup to reduce the startup cost. Rule 8 of [`../network/storage-ownership.md`](../network/storage-ownership.md)
  still removes stored entries whose publisher is not the owner.

## Open points

The following weakness can be removed by a change of other code. The developers have not decided this change yet.

- **Types without the check of rule 6.** `AuthorizedBondedRolesService` adds every `AuthorizedOracleNode` to its list
  of oracle nodes, and `AccountTimestampService` applies every `AuthorizedAccountTimestamp`, without
  `hasAuthorizedPubKey`. For data without the static flag, the network storage checks only the signature (rule 4), so
  any key can publish such data.
  - What removes it: the check of rule 6 for these two types.

## Verification

- With the real `DataService` and `AuthorizedBondedRolesService`: a store holds a root role with the static flag
  whose key is not a static key. The application does not accept the role, and it does not accept a role which that
  key authorizes.
- A root role whose key is a static key passes the check of the application.

## Related code

- `AuthorizedData.isNotAuthorized()` (rules 1 and 2 at add), `AuthenticatedDataStorageService.add()`.
- `AuthorizedBondedRolesService.hasAuthorizedPubKey()` (rules 2 and 3 before use).
- `AuthorizedPubKeys` (the static keys).
