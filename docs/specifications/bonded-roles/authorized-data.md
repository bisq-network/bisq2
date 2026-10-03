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
- **Static key requirement**: a type of authorized data can require a static key
  (`AuthorizedDistributedData.isStaticPublicKeyRequired()`). Then only the static keys of the type may authorize its
  data, with and without the static flag. The type declares the requirement in the code, not the data, because the
  publisher sets the fields of the data. Rule 16 defines which types require a static key.
- **Authorizing role type**: the type of bonded role which may authorize a type of data. For example, an oracle node
  authorizes bonded roles and reputation data, and a security manager authorizes alerts.

## Rules

1. The authorization signature must be valid for the authorized public key and the data.
2. If the data has the static flag, or if its type requires a static key, the authorized public key must be one of the
   static keys of the type of the data.
3. If the data does not have the static flag and its type does not require a static key, the authorized public key must
   be the authorized key of a bonded role of the authorizing role type. A banned role does not count, unless the node is
   configured to ignore the security manager.

### Where a node checks the rules

4. The network storage checks rules 1 and 2 when it adds the data. It checks rule 2 first, because rule 2 needs no
   signature verification. It does not know the bonded roles, so it does not check rule 3.
5. When the network storage loads a store, it removes the data which fails rule 2. It does not verify the
   authorization signatures of the stored data again (rule 1).
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
do not need it. The configuration option `application.bondedRoles.onlyStaticOracleNodes` enables the restriction. It
is `true` in the configuration files of all applications, and a node which no configuration sets uses `true` as well.
While it is `true`, these rules apply:

10. If the authorizing role type is the oracle node, the authorized public key must be an oracle node key. This applies
    to data with and without the static flag, and it replaces rule 3 for this authorizing role type. Rules 8, 9 and 18
    and the counting of publications in [`registration.md`](registration.md) use this rule.
11. A node lists a bonded role of the oracle node role type only if the authorized public key of the role is an oracle
    node key.
12. An oracle node does not accept a new registration of a role of the oracle node role type whose authorized public key
    is not an oracle node key. It still accepts the cancellation of such a role.
13. An oracle node does not start if its own authorized public key is not an oracle node key.
14. Data which fails rule 10 is not checked again when a bonded role is added (rule 7), because the static keys do not
    change while the node runs.

When the option is `false`, rules 10 to 14 and 16 do not apply, and the other rules apply unchanged.

### The holder of authorized data

15. Authorized data is valid only inside `AuthorizedData`, which carries the authorized public key and the
    authorization signature. A node rejects default authenticated data whose data is authorized data when it constructs
    or decodes it (see [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md)). So no node of this
    version stores, relays, serves or uses such data, and a store skips such a persisted entry when it loads.

### Oracle node keys in the network storage

16. While the option `onlyStaticOracleNodes` is `true`, the types whose authorizing role type is the oracle node
    require a static key (rule 2): `AuthorizedOracleNode`, `AuthorizedBondedRole`, `AuthorizedMarketPriceData`,
    `AuthorizedBurningmanListByBlock`, `AuthorizedAccountTimestamp`, `AuthorizedAccountAgeData`,
    `AuthorizedSignedWitnessData`, `AuthorizedTimestampData`, `AuthorizedProofOfBurnData` and
    `AuthorizedBondedReputationData`. So the network storage applies rule 10 as well: it rejects their data of other
    keys when it adds it (rule 4), and it removes such data when it loads a store (rule 5). For `AuthorizedOracleNode`,
    the rule concerns the authorized public key, not the publisher. The oracle node still publishes it with the key of
    its network ID (rule 9 of [`../network/storage-ownership.md`](../network/storage-ownership.md)).

### The configuration option `onlyStaticOracleNodes`

17. All nodes of a network use the same value of the option. A node sets the value when it starts, before its stores
    load their data, and does not change it while it runs.

### Several publications of the same data

18. Several keys can publish equal data, each with its own authorized key. For example, the alert view of the desktop
    publishes every alert again with the key of the security manager who opens it, and several oracle nodes publish
    the same bonded role. A node which holds one item for equal data keeps the item while the network storage holds at
    least one publication of it for which rule 2 or rule 3 holds at that time, with the authorizing role type of the
    data. When a publication is removed or expires, the node removes the item only if no such publication remains. It
    does not check the removed publication itself. This applies to:
    - bonded roles (see [`registration.md`](registration.md#several-oracle-nodes)), oracle nodes (rule 8) and account
      timestamps (rule 9);
    - alerts and difficulty adjustments, with the security manager as authorizing role type;
    - release notifications, with the release manager as authorizing role type;
    - bans of user profiles, with the moderator as authorizing role type;
    - reputation data, including profile timestamps: see
      [`../security/derived-state.md`](../security/derived-state.md#reputation).

## Reasons

- Rule 6: a later version can remove a static key, for example after the key was compromised. The network storage
  removes the data of that key when it loads a store (rule 5), and the applications check the key again before they
  use the data. The applications do not rely on the network storage, because a gap in its checks would otherwise give
  authority to data which no valid key authorized (for example the gap which rule 15 closes). The root role of an
  oracle node has the static flag. With the root role of a removed key, that key could authorize bonded roles of every
  type on this node, because rule 3 would find its role.
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
  for non-static oracle nodes stays, so that a network can allow them again with the configuration option.
- Rule 10 does not depend on the static flag, because the publisher sets the flag.
- Rule 11: the role list shows the oracle nodes to the users, and monitoring tools use it to find the oracle nodes. A role
  of another key would show an oracle node which no node accepts.
- Rules 12 and 13 stop an oracle node from publishing data which no node with the restriction accepts. Rules 10 and 11
  do not depend on them, because oracle nodes of earlier versions still accept such registrations.
- Compatibility: nodes of earlier versions still accept non-static oracle nodes. Oracle nodes with an oracle node key
  publish their data with the static flag, which all versions accept.
- Rule 2, static key requirement: the publisher sets the static flag. If only the flag required a static key, any key
  could authorize data without the flag, and the network storage would accept it (rule 4). The applications ignore
  such data, but all nodes would store, relay and serve it, up to the limit of the store. For a type whose authorizing
  role type has only static keys, such data is never valid, so the type declares the requirement, and the network
  storage rejects the data of other keys.
- Rule 4, order: rule 2 is a lookup in a set of a few keys, and rule 1 costs a signature verification. With rule 2
  first, a node rejects data of other keys cheaply, also when a node of an earlier version sends much of it in an
  inventory response.
- Rule 4, data which fails rule 2 is no sign of an attack by the peer: nodes of earlier versions accept data of other
  keys for types which require a static key now, and a later version can remove a static key. Honest nodes of earlier
  versions then relay such data. A node of this version therefore does not log each such rejection as a warning. An
  invalid authorization signature (rule 1) is not caused by a difference between versions.
- Rule 5: a node which updates can hold data which this version rejects: data of other keys of a type which requires
  a static key now, and data of a static key which this version removed. If the store kept it, the node would serve it
  in its inventory responses until it expires, up to 100 days. Other nodes of this version reject it, so they receive
  it again with each response of that node. If a full response contains nothing else which they accept, they stop
  requesting inventory from that node (rule 6 of
  [`../network/inventory-hash-set-filter.md`](../network/inventory-hash-set-filter.md#repeated-requests)). Rule 2 needs
  no signature verification, so the check costs little when the store loads. The static keys do not change while the
  node runs, so the store does not need to check rule 2 again later.
- Rule 15: the network storage checks rules 1 and 2 only for data inside `AuthorizedData`. As default authenticated
  data, authorized data would have no authorization at all. The owner rule rejects such data if it names no owner (rule
  3 of [`../network/storage-ownership.md`](../network/storage-ownership.md)). `AuthorizedOracleNode` names its owner,
  the key of its network ID. So without rule 15, any node could publish an `AuthorizedOracleNode` with its own network
  ID and any key as default authenticated data. All nodes would store, relay and serve it with the highest priority,
  although no key authorized it. Nodes of earlier versions do not create such data, so the rule is checked at parsing
  (rule 3 of [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md)).
- Rule 16: without it, any node could publish data of these types with its own key and without the static flag. All
  nodes stored, relayed and served it until it expired. Only the proof of work of each add request and the limit of
  100 000 entries per store limited it. Several of these types have the highest priority in inventory responses, which
  have a size limit, so a node which synchronizes received such data first and could miss other data. The applications
  ignore such data (rule 10), so the rejection in the network storage changes no decision of an application.
- Rule 16, why the network storage can apply rule 10: while the option is `true`, rule 10 does not depend on the bonded
  roles. The static keys do not change while a node runs, so data of another key cannot become valid later (rule 14).
  This is not true for rule 3, which the network storage still does not check.
- Rule 16, the authorized key and not the publisher: the authorization signature binds the authorized key to the
  data, also to the network ID of an `AuthorizedOracleNode`. So only the holder of an oracle node key can create such
  data, also if another key publishes it. The owner rules of
  [`../network/storage-ownership.md`](../network/storage-ownership.md) stay unchanged: nodes of v2.1.13 and earlier
  accept an `AuthorizedOracleNode` only from the key of its network ID. A second publication, a new version or a new
  type of `AuthorizedOracleNode` is not needed for rule 16.
- Rule 16, the type declares the requirement: the network module does not know oracle nodes. It knows only that a
  type can require its static keys (`AuthorizedDistributedData.isStaticPublicKeyRequired()`). The types which oracle
  nodes authorize implement `OracleNodeAuthorizedData` of the bonded roles module, which returns the value of the
  option `onlyStaticOracleNodes`. The decision stays in the bonded roles domain, and the dependency points from the
  bonded roles module to the network module.
- Rule 16, compatibility: no field, message, signature or storage key changes. Nodes of earlier versions accept data
  of other keys and relay it. A node of this version rejects it without verifying a signature and keeps the connection
  (rule 4). It does not relay it. When a node of an earlier version serves such data in an inventory response, the
  requester skips it. If a full response contains nothing else which the requester accepts, the requester stops
  requesting inventory from that node (rule 6 of
  [`../network/inventory-hash-set-filter.md`](../network/inventory-hash-set-filter.md#repeated-requests)). Oracle nodes
  of this version publish only with oracle node keys (rule 13), so rule 16 rejects no data of an honest oracle node of
  this version. An oracle node of an earlier version with another key is not an oracle node for nodes of this version
  (rule 10). Nodes of this version also stop relaying its data, so nodes of earlier versions receive it only from
  other nodes of earlier versions.
- The restriction is a configuration option, not a constant in the code (developer decision of 2026-10-04).
- Rule 17: a node with another value than the other nodes disagrees with them about which oracle nodes, bonded roles
  and reputation data are valid (rules 10 and 11), and its network storage accepts other data (rule 16). With `false`
  in a network of nodes with `true`, it stores, relays and serves data of other keys which the other nodes reject, and
  as an oracle node it accepts registrations of oracle node roles of other keys and starts with another key (rules 12
  and 13). The other nodes ignore the data of such an oracle node. With `true` in a network which allows non-static
  oracle nodes, the node rejects their data. So the value is part of the definition of a network, like its seed nodes.
- Rule 17, set at start: the network storage reads the value through the type of the data (rule 16), so the node keeps
  it in one setting for the whole node (`OnlyStaticOracleNodes`). The stores check rule 2 when they add data and when
  they load (rules 4 and 5). A value which changed while the node runs would leave data in the stores which they
  would not accept now, or would reject data which they accepted before.
- Rule 5, signatures: verifying the authorization signature of every stored entry costs one signature verification
  per entry. This check was disabled at startup to reduce the startup cost. Rule 8 of
  [`../network/storage-ownership.md`](../network/storage-ownership.md) still removes stored entries whose publisher is
  not the owner.
- Rule 18: a node which starts derives its items from the stored publications. Before rule 18, the removal or expiry of
  one publication removed the item from the running nodes, while nodes which started later still had it from another
  publication. Running and restarted nodes then disagreed, for example about a ban or about an alert which halts
  trading.
- Rule 18 does not check the removed publication. A removal of a publication whose key has lost its authority, for
  example the key of a banned security manager, still removes an item which no remaining publication supports. A node
  which starts would not hold it either. The removal of a copy of another key removes no item while an authorized
  publication remains.
- Rule 18 does not decide who publishes and removes data. With the alert view of the desktop, every security manager
  who opens the view publishes all alerts again, so an alert stays until each of them removes his publication. Release
  notifications behave the same way (see [Open points](#open-points)).

## Known limitations

- **Data of other roles.** Security managers, release managers and moderators can hold a role whose key is not a
  static key (rule 3). The network storage does not know the bonded roles, so it accepts their data of any key without
  the static flag: alerts, difficulty adjustments, minimum reputation scores, release notifications and bans. All
  nodes store, relay and serve such data, and the applications ignore it.
  - Why it cannot be removed now: bonded roles can arrive after the data which they authorize (rule 7), and the
    network storage does not know them. A check of rule 3 when the network storage adds the data would reject valid
    data which arrives before its role.
  - What would remove it: only static keys for these roles as well, or data which carries the proof of its authority,
    for example the bonded role of its key with the authorization of an oracle node key. Then the network storage can
    check the authority without knowing the bonded roles.
  - Who decides: the developers.
- **Nodes of earlier versions** store, relay and serve data of other keys of the types of rule 16 until they update.
  Their inventory responses can be full of such data.
- **The configuration option.** A node with `onlyStaticOracleNodes=false` does not apply rule 16, and its network
  storage accepts data of the types which oracle nodes authorize from every key again (rule 4). Before the default
  changes, the developers must decide how the network storage limits such data, for example with a proof of authority
  as described above.

## Open points

- **Who can remove an alert or a release notification.** The alert view of the desktop publishes every alert again
  with the key of each security manager who opens it. The view of the release manager does the same with the release
  notifications. With rule 18, an alert then stays until every security manager who opened the view removes his
  publication, and the same holds for a release notification. Before rule 18, the removal by one of them removed the
  data only on the running nodes.
  - Decision needed: should these views publish only the data of the user who opens them, so that only their
    publisher removes them? Or should any holder of the role be able to remove every publication of the data?
  - Who decides: the developers, together with the security managers and release managers.

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
- Rules 2, 4 and 5 with a test type which requires a static key: the network storage rejects its data of another key,
  with and without the static flag, and accepts its data of a static key without the static flag. It accepts data of
  another key without the static flag of a type without the requirement, and rejects such data with the static flag.
  It checks the key before the signature: data of another key with an invalid signature fails rule 2, and the
  rejection is no severe failure. An invalid signature of a static key is a severe failure. When the store loads, it
  removes the data which fails rule 2 and keeps the other data.
- Rule 15: default authenticated data whose data is authorized data cannot be created, and a received one fails at
  parsing. This includes the real `AuthorizedOracleNode` published by the key of its network ID.
- Rule 16: every authorized type is classified. The ten types which oracle nodes authorize require the oracle node
  keys, the types of the other roles do not. With the real store: an `AuthorizedOracleNode` with an oracle node key as
  authorized key and the key of its network ID as publisher is accepted. One with another authorized key is rejected,
  and the rejection is no severe failure. A bonded role and a profile timestamp of another key are rejected, and a role
  of a removed static key is removed when the store loads. The applications still ignore such data when a store holds
  it (rule 6).
- Rule 17: the configuration of the bonded roles contains the option. With the default, the network storage rejects a
  bonded role of another key, and the bonded role service of the application does not allow another oracle node key.
  With `false`, the network storage accepts such a role without the static flag, the service allows the key, and the
  types which oracle nodes authorize do not require a static key.
- With the restriction (rules 10 and 11): an oracle node of an oracle node key is listed, with and without the static
  flag. An oracle node of a key with a role of the oracle node role type is not listed. A role of the oracle node role
  type of another key is not listed, and its key does not authorize other roles.
- With the restriction (rule 12): an oracle node does not accept a new registration of an oracle node role of another
  key, and it accepts its cancellation.
- With the real stores (rule 18): if two security managers publish the same alert, the removal of one publication keeps
  the alert and a ban which it applies, and the removal of the last publication removes them. The same holds for
  release notifications, difficulty adjustments and bans of user profiles. The removal of a copy of another key keeps
  the item. If two oracle nodes publish the same timestamp of a profile, the removal of one keeps the profile age. If
  their timestamps differ, the removal of the older one gives the profile the newer one, and the removal of the last
  removes the profile age.

## Related code

- `AuthorizedDistributedData.isStaticPublicKeyRequired()` (static key requirement).
- `AuthorizedData.isAuthorizedKeyAllowed()` (rule 2), `AuthorizedData.isAuthorizationSignatureInvalid()` (rule 1),
  `AuthenticatedDataStorageService.add()` (rule 4) and `AuthenticatedDataStorageService.isToPrune()` (rule 5).
- `DefaultAuthenticatedData.verify()` (rule 15).
- `OracleNodeAuthorizedData` (rule 16).
- `AuthorizedBondedRolesService.hasAuthorizedPubKey()` (rules 2 and 3 before use).
- `ProfileAgeService.getTimeStampedProfileIds()` (rule 6, profile timestamp request lookup).
- `AuthorizedBondedRolesService.getAuthorizedOracleNodes()` (rule 8), `AccountTimestampService` (rule 9).
- `AuthorizedBondedRolesService.getAuthorizedPublishedData()` and `hasAuthorizedPublication()` (the lookup of rules 8, 9
  and 18), `onAuthorizedDataRemoved` of `AlertService`, `DifficultyAdjustmentService`, `ReleaseNotificationsService`
  and `BannedUserService` (rule 18), `SourceReputationService.onAuthorizedDataRemoved` (reputation data,
  `../security/derived-state.md`).
- `BondedRolesService.Config` and `OnlyStaticOracleNodes` (the configuration option, rule 17),
  `AuthorizedBondedRolesService.isOracleNodeKeyAllowed()` (rules 10 and 11),
  `Bisq1BridgeRequestService.processBondedRoleRegistrationRequest()` (rule 12), `OracleNodeService` (rule 13).
- `AuthorizedPubKeys` (the static keys, `getOracleNodePubKeys()` for the oracle node keys).
