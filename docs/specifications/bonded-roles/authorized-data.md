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
   This also applies when a node looks up stored profile timestamps (`AuthorizedTimestampData`) to decide which
   profiles need a timestamp request. Only timestamps authorized by an oracle node count as existing timestamps,
   using the same authorization check as applying profile age. The renewal interval for authorized timestamps is
   unchanged.
7. If no bonded role matches (rule 3), the application keeps the data and checks it again when a bonded role or an
   oracle node is added. Bonded roles can arrive after the data which they authorize. The application keeps the data
   only while the network storage holds it: when the data is removed or expires, the application does not check it
   again.
8. A node lists an oracle node (`AuthorizedOracleNode`) only while the network storage holds at least one publication
   of it for which rule 2 or rule 3 holds at that time, with the oracle node as authorizing role type. The node derives
   the list from the stored publications each time it uses it. So the removal of another publication does not remove
   an oracle node, and a ban or the removal of the role which authorizes its key removes it.
9. A node applies an account timestamp (`AuthorizedAccountTimestamp`) only if rule 2 or rule 3 holds for it, with the
   oracle node as authorizing role type. The same applies when it removes an account timestamp, and when it looks up
   the stored account timestamp of its own account to decide whether to request it again. It keeps an applied account
   timestamp while the network storage holds at least one publication of it for which rule 2 or rule 3 holds, for
   example of another oracle node.

### Temporary restriction to static oracle node keys

An **oracle node key** is one of the static keys of the oracle node role type. A **non-static oracle node** is an
oracle node whose key is not an oracle node key, and whose authority comes from a bonded role of the oracle node role
type (rule 3). Oracle nodes with an oracle node key can register a role of the oracle node role type as well, but they
do not need it. The application has a flag in the code, `AuthorizedBondedRolesService.ONLY_STATIC_ORACLE_NODES`. It is
currently `true`. While it is `true`, these rules apply:

10. If the authorizing role type is the oracle node, the authorized public key must be an oracle node key. This applies
    to data with and without the static flag, and it replaces rule 3 for this authorizing role type. Rules 8 and 9 and
    the counting of publications in [`registration.md`](registration.md) use this rule.
11. A node lists a bonded role of the oracle node role type only if the authorized public key of the role is an oracle
    node key.
12. An oracle node does not accept a new registration of a role of the oracle node role type whose authorized public key
    is not an oracle node key. It still accepts the cancellation of such a role.
13. An oracle node does not start if its own authorized public key is not an oracle node key.
14. Data which fails rule 10 is not checked again when a bonded role is added (rule 7), because the static keys do not
    change while the node runs.

When the flag is `false`, rules 10 to 14 do not apply, and rules 1 to 9 apply unchanged.

## Reasons

- Rule 6: a later version can remove a static key, for example after the key was compromised. A node which updates
  still holds the data which that key published, for up to 100 days, and the store does not check it again (rule 5).
  If the application relied on the check of rule 4, it would still use this data. The root role of an oracle node has
  the static flag. With the root role of a removed key, that key could authorize bonded roles of every type on this
  node, because rule 3 would find its role.
- Rule 6 needs no signature verification. It is a lookup in a set of a few keys.
- Rule 6 also protects the profile timestamp request lookup. A signed timestamp from another key can be stored
  without oracle authority when it has no static flag. Counting it as an existing timestamp would suppress a
  missing-timestamp request while renewal is not due, even though the node rejects it when applying profile age.
- Rule 7, removed data: data which is checked again after its removal would reach the services although no store
  holds it, for example an alert which its publisher removed, and it would stay until the node shuts down. Any key
  can publish data without the static flag (rule 4), so the kept data would also grow without limit. With rule 7, the
  limits of the stores bound it.
- Rule 8: users send their registration requests and account timestamp requests to the listed oracle nodes. Without
  the check, any key could publish an oracle node without the static flag, and users would send it these requests.
  A list which the node updates only when oracle node data is added or removed would keep an oracle node whose role a
  security manager bans later, or whose role is removed. Users would still send it their requests.
- Rule 9: an account timestamp states the age of a payment account, which other users trust. Without the check, any
  key could publish an earlier date for the hash of an account, or remove the applied timestamp with a copy of
  another key. A copy with a recent date could also stop the owner of the account from requesting its timestamp again
  before the timestamp of the oracle node expires. Several oracle nodes can publish the same account timestamp, each
  with its own key. The node holds one account timestamp per hash, so the removal or expiry of one publication must
  not remove it while another publication remains.
- Rules 10 to 13: for now, only the oracle node keys in the released code may act as oracle nodes. Otherwise an oracle
  node could register further oracle nodes with non-static keys, and the authority over all data which oracle nodes authorize (bonded roles,
  reputation, account timestamps, market prices, burningman data) would depend on a chain of registrations. The code
  for non-static oracle nodes stays, so that a later release can allow them again by changing the flag.
- Rule 10 does not depend on the static flag, because the publisher sets the flag.
- Rule 11: the role list shows the oracle nodes to the users, and monitoring tools use it to find the oracle nodes. A role
  of another key would show an oracle node which no node accepts.
- Rules 12 and 13 stop an oracle node from publishing data which no node with the restriction accepts. Rules 10 and 11
  do not depend on them, because oracle nodes of earlier versions still accept such registrations.
- The flag is a constant in the code, not a configuration option. All nodes of a release must apply the same rule.
  Otherwise nodes disagree about which bonded roles and which reputation data are valid.
- Compatibility: nodes of earlier versions still accept non-static oracle nodes. Oracle nodes with an oracle node key
  publish their data with the static flag, which all versions accept.
- Rule 5: checking the authorization of every stored entry verifies one signature per entry. This check was disabled
  at startup to reduce the startup cost. Rule 8 of [`../network/storage-ownership.md`](../network/storage-ownership.md)
  still removes stored entries whose publisher is not the owner.

## Verification

- With the real `DataService` and `AuthorizedBondedRolesService`: a store holds a root role with the static flag
  whose key is not a static key. The application does not accept the role, and it does not accept a role which that
  key authorizes.
- A root role whose key is a static key passes the check of the application.
- An oracle node of a known oracle node role is listed. An oracle node of an unknown key, or with the static flag and a
  key which is no static key, is not listed. The removal of a publication of another key does not remove a listed
  oracle node. An oracle node whose role is banned later is no longer listed.
- An account timestamp of an oracle node is applied. An account timestamp of another key is not applied, and its
  removal does not remove the applied one. If two oracle nodes publish the same account timestamp, the removal of one
  publication keeps it, and the removal of the last publication removes it.
- With the real `DataService` and `AuthorizedBondedRolesService`, the profile timestamp request lookup ignores a
  stored timestamp signed by a non-oracle key and counts one signed by an oracle key, even before profile age is
  applied. With mixed publications, only profiles with an authorized timestamp count. Removing the last authorized
  timestamp makes the profile missing again, even if an unauthorized copy remains in storage.
- With the restriction (rules 10 and 11): an oracle node of an oracle node key is listed, with and without the static
  flag. An oracle node of a key with a role of the oracle node role type is not listed. A role of the oracle node role
  type of another key is not listed, and its key does not authorize other roles.
- With the restriction (rule 12): an oracle node does not accept a new registration of an oracle node role of another
  key, and it accepts its cancellation.

## Related code

- `AuthorizedData.isNotAuthorized()` (rules 1 and 2 at add), `AuthenticatedDataStorageService.add()`.
- `AuthorizedBondedRolesService.hasAuthorizedPubKey()` (rules 2 and 3 before use).
- `ProfileAgeService.getTimeStampedProfileIds()` (rule 6, profile timestamp request lookup).
- `AuthorizedBondedRolesService.getAuthorizedOracleNodes()` (rule 8), `AccountTimestampService` (rule 9).
- `AuthorizedBondedRolesService.ONLY_STATIC_ORACLE_NODES` and `isOracleNodeKeyAllowed()` (rules 10 and 11),
  `Bisq1BridgeRequestService.processBondedRoleRegistrationRequest()` (rule 12), `OracleNodeService` (rule 13).
- `AuthorizedPubKeys` (the static keys, `getOracleNodePubKeys()` for the oracle node keys).
