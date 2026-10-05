# Bonded role registration and DAO revalidation

## Scope

This specification defines how a Bisq 2 oracle accepts, publishes, persists, cancels and revalidates
a bonded-role registration against the Bisq 1 DAO. The Bisq 1 specification remains authoritative
for proposal selection, valid role lockups, registration-message construction and signature
verification.

## Registration protocols

A registration request carries an explicit protocol version.

Network-message validation is deliberately limited to structural and size checks. Exact semantics
for protocol versions understood by a node are enforced when the request enters the registration
domain. A node must ignore an unsupported future protocol rather than treating it as proof that a
registration is invalid. Persisted unsupported registrations are retained until software which
understands them can revalidate them.

### Version 1: deployed legacy registrations

Version `1` is the historical format. It contains the bond user name, role type, Bisq 2 profile id
and the lockup-input-key signature over the profile id. It contains empty proposal and lockup
transaction ids.

Version `1` exists only to retain and cancel registrations which an oracle had accepted before the
version-2 rollout. An oracle must not create a new authorization from a previously unknown version-1
request, even when the Bisq 1 bridge can still verify an eligible pre-cutoff lockup. Existing
network-data version-1 authorized roles are imported into the oracle's persistent registration store
as registration protocol version `1` during migration. Because the protocol-version field was added
after those messages were deployed, it has explicit protobuf presence and an absent value is
normalized to legacy version `1` when read; an explicitly supplied version `0` remains unsupported.
New messages always serialize an explicit supported version.

### Version 2: proposal-key ownership

Every new registration uses version `2` and identifies:

- the accepted role's bond user name and role type;
- the Bisq 2 profile id and authorization public key;
- the canonical proposal transaction id;
- the exact lockup transaction id backing the registration; and
- the proposal-key signature returned by Bisq 1.

Both transaction ids are required 64-character Bitcoin transaction ids. Bisq 2 canonicalizes them
to lowercase before constructing the request because Bisq 1 transaction identities and the signed
registration message use their lowercase representation. The oracle submits the complete request to
the bridge. It publishes an authorization only when the bridge returns no error.

The proposal and lockup binding is security-critical state. A newly published
`AuthorizedBondedRole` therefore carries the binding in version-2 network data, and those fields are
included in the oracle-authorized hash. Existing version-1 registrations remain version-1 network
data with empty binding fields. A network peer which does not understand version-2 authorized-role
data cannot validate a new bound role; oracle nodes and consuming Bisq 2 nodes must therefore be
upgraded before new version-2 registrations are relied upon.

Network-data versions and registration-protocol versions evolve independently. Their historical
mapping is immutable: network-data versions `0` and `1` carry registration protocol `1`, while
network-data version `2` carries registration protocol `2`. A node does not apply these historical
mapping checks to an unknown future network-data version. It may continue consuming the fields it
understands when the oracle-authorized hash remains compatible; hash-incompatible data fails the
oracle signature check.

## Sender of a registration

A user sends a registration or a cancellation as a confidential message to each oracle node. The oracle node knows
the key which signed the message, the sender key. Before it asks the Bisq 1 bridge to verify the bond, it checks:

1. The profile ID of the request is the hash of the sender key. So only the owner of a profile can register or cancel
   a role of the profile.
2. The key of the network ID of the request is the sender key. The role data contains this network ID.

Reasons:

- The bond proves who holds the role in the DAO. The sender key proves who holds the Bisq 2 profile.
- Rule 2: the identity of a registration leaves out the network ID (see
  [Persistence and publication](#persistence-and-publication)), but the role data contains it. Without rule 2, the
  owner of a profile could send one registration with the network IDs of other keys, and each of them would become a
  separate role record. Rule 2 limits the network IDs of a registration to the key of the profile. See
  [Open points](#open-points) for the variants which remain.

Compatibility: released clients (since 2.0.0) send a registration with the key pair of the selected profile, and the
network ID of the request is the network ID of this key pair. So oracle nodes accept their requests.

## Addresses of node roles

A **node role** is a bonded role of a node type: seed node, oracle node, explorer node or market price node.

1. The role data of a node role (`AuthorizedBondedRole`) contains the addresses of the node. Role data of a node role
   without addresses is invalid. A node rejects it when it constructs or decodes it (rule 1 of
   [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md)). The role data of another role type
   may leave the addresses out.
2. The registration request is not checked for this rule at parsing. An oracle node accepts a registration only if it
   can create valid role data from it. It checks this before it asks the Bisq 1 bridge to verify the bond. So it
   neither persists nor publishes the registration of a node without addresses. A cancellation needs no role data.
3. An oracle node of an earlier version could persist the registration of a node without addresses. When the bridge
   reports such a registration as invalid, or when its holder cancels it, the oracle node removes the persisted
   registration. It cannot create the removal of the role data, and nodes of this version do not hold such role data.

Reasons:

- Nodes reach a node at the addresses of its role. A node adds the addresses of every seed node role to its seed
  nodes, and the role list and the node monitor show the addresses of all node roles. Without rule 1, the processing
  of a seed node role without addresses fails when a node adds or removes the role. It fails after the node listed the
  role, so the node lists a seed node which it cannot reach. Nodes of v2.1.13 do not process each item of their startup
  replay on its own (see [`../network/startup-replay.md`](../network/startup-replay.md)), so such a role stops their
  start.
- Rule 1 is an integrity rule which a node can decide from the role data alone, so the role data checks it in
  `verify()`. A node of this version then never stores, relays or uses such role data, also if an oracle node of an
  earlier version published it. An oracle node of this version does not load such role data from its store either, so
  it does not publish it again.
- Rule 2: the store of the persisted registrations of an oracle node fails to load as a whole if one entry cannot be
  parsed (see [`../reputation/account-age.md`](../reputation/account-age.md#persisted-request-compatibility)). A check
  of the registration request at parsing would make this store fail if an oracle node of an earlier version persisted
  such a registration.
- Rule 3: without it, one such registration would stop the deactivation of the registrations after it in the same
  revalidation.

Compatibility:

- Released clients send the addresses of a node with its registration. The registration form of the desktop requires
  an address file for every node type, and oracle and seed nodes publish their own role with the addresses of their
  network ID. So nodes of this version drop no role data which an oracle node created for the registration of a
  released client with a valid address file.
- Nodes of v2.1.13 and earlier accept a node role without addresses, and a seed node role without addresses stops them
  at startup. Updated oracle nodes no longer publish such role data, and updated nodes do not relay it. Role data which
  an oracle node of an earlier version published stays in the stores of nodes of earlier versions until it expires.

## Persistence and publication

Before publishing a successful registration, each oracle persists the complete non-cancellation
request in its private bridge-request store. The persisted request is the source for subsequent
revalidation and removal.

The oracle publishes and removes the authorization with its authorized key pair, because the
authorized key owns authorized data in the network storage (see
[`../network/storage-ownership.md`](../network/storage-ownership.md)). Oracles of earlier versions
published it with the key pair of their network identity. At startup, the oracle publishes such
copies again with its authorized key pair and removes them. A cancellation request is not stored as a registration and is ignored if
encountered while loading persisted registration state.

Multiple registrations for the same public role may coexist when they have distinct signed
proposal/lockup bindings. They are independent. A cancellation removes only the persisted
registration whose identity, signature, protocol version and transaction binding match the request.
Changes to a node's transport addresses do not change that logical registration identity; if several
published transport-address variants exist, cancellation or invalidation removes all of those copies
without affecting a different signed binding.

The oracle recovers its own persisted registration records from its authorized network data during
startup migration. Only network-data versions whose removal hash can be reproduced exactly are
recovered. In particular, obsolete network-data version `0` is not reconstructed as version `1`,
and unknown future versions are not imported into an older oracle's registration store. Recovery
must trust the outer authorized-data signing key, not the informational
authorizing-oracle field, because the latter is excluded from the distributed-data hash.
Static root roles are bootstrap authority, not bridge registrations, and must never be imported into
the DAO-revalidation store. This distinction is made from the configured root identity rather than
the `staticPublicKeysProvided` transport field, which is intentionally excluded from the data hash.

## Several oracle nodes

Users send registration requests to all oracle nodes, and each oracle node publishes the roles which it validated with
its own authorized key. So a role can have several publications in the network storage, one per oracle node. The
equality of a role leaves out the oracle node which authorized it, so a node shows one role for all of them.

- A node keeps a role while the network storage holds at least one publication of an equal role which an oracle node
  authorized. The removal or the expiry of one publication does not remove the role.
- Each oracle node removes only its own publication, after its own DAO revalidation or cancellation (see below).
- A publication counts only if an oracle node authorized it: its authorized key is a hard-coded oracle key of the
  version of the node, or the key of a known oracle node role. This is the same check as for every use of authorized
  data (rules 2 and 3 of [`authorized-data.md`](authorized-data.md)). While only static oracle nodes are allowed,
  only the hard-coded oracle keys count (rule 10 of [`authorized-data.md`](authorized-data.md)). A publication of a key which a later version
  removed from the hard-coded keys does not count. The store removes it when it loads (rule 5 of
  [`authorized-data.md`](authorized-data.md)), but the count does not rely on that.

Reasons:

- The stores remove publications independently of each other: at expiry, and from the cutoff date of signature V1 on
  also the copies without signature V2 which nodes of earlier versions relayed (see
  [`../network/storage-request-signatures.md`](../network/storage-request-signatures.md#cutoff-date-of-signature-v1)).
- An oracle node removes its own publication of a role which disappears from its view of the bonded roles. If the
  removal of one publication removed the role, the removal of a copy of the publication of oracle node A on oracle
  node B would make B remove its own publication for all nodes, and A would follow. All nodes would then hold only
  removals of the role, and its holder would have to register again.
- A role which one oracle node removes stays while another oracle node publishes it. Both oracle nodes check the same
  DAO state and receive the same cancellation requests, so the other oracle node removes its publication as well.

## DAO revalidation

At startup, after every live DAO block which advances the contiguous bridge view and after every
successful historical catch-up, the oracle sends one batch containing all registrations it still
considers active. The Bisq 1 bridge evaluates the batch against one DAO-state snapshot and returns
its block height and one result in request order.

- The response cardinality must equal the request cardinality. Otherwise the oracle ignores the
  malformed response and retains every registration.
- A result without an error leaves the corresponding authorization unchanged.
- A result with an error removes only that exact oracle-published authorization and then removes its
  persisted request. This covers confirmed unlocks, expired unlocks, confiscation, illegal spending,
  invalid lockups and no-longer-canonical proposals.
- A transport error, deadline expiry or unavailable bridge does not deactivate registrations. The
  next startup or live-block trigger retries an authoritative snapshot. Every unary verification has
  a bounded deadline so one stalled bridge call cannot permanently stop revalidation.
- Live block notifications include blocks without proof-of-burn or bonded-reputation records. A
  successful historical catch-up also triggers revalidation at its completed snapshot height even
  when the sparse response contains no exported transaction.

Concurrent block triggers are coalesced, but a trigger received during an in-progress request must
cause one more snapshot after that request completes.

## Legacy migration and rollout

The Bisq 1 bridge accepts registration protocol `1` only for the legacy mainnet window defined by
the Bisq 1 bonded-role specification. Therefore post-cutoff mainnet registrations, and all legacy
registrations on non-mainnet networks, must be re-registered with protocol `2` before destructive
DAO revalidation is enabled for that deployment.

### Mainnet activation sequence

Publishing network-data version `2` is a coordinated rollout boundary because earlier Bisq 2 peers
cannot verify its hash. Mainnet activation must use the following order:

1. Deploy the upgraded Bisq 2 oracle application with the operator-controlled
   `bondedRoleRegistrationEnabled` setting explicitly documented for the deployment. The code default
   is `true` because only the oracle operator can set this configuration; an ordinary Bisq peer cannot
   enable admission by changing a desktop setting or constructing a network request. During a
   staggered bridge/client migration, operators must either set the deployment override to `false` or
   maintain an explicit hold in which eligible proposal-key holders do not submit new registrations.
   Cancellation, DAO revalidation where available and republishing of existing authorized data remain
   enabled independently of new admission.
2. Release the compatible Bisq 1 bridge and enforce that Bisq 1 update. The Bisq 1 release and every
   later release must retain this protocol-2 verification path; it must not be treated as a temporary
   migration shim.
3. Release Bisq 2 with registration protocol `2` and network-data version `2` support. Upgrade every
   project-operated consumer of bonded-role data. Updated oracles reject unknown version-1
   registrations, while known version-1 records remain cancellable, revalidatable and renewable.
   After all required consumers are upgraded, operators may keep the code default
   `bondedRoleRegistrationEnabled=true` and lift the operational hold once the remaining checks are
   complete.
4. Enforce the new Bisq 2 minimum version before the first version-2 registration is submitted. The
   minimum-version mechanism prevents an older client from continuing to trade; it does not disconnect
   that process or prove that every desktop, API application or headless infrastructure node has
   upgraded. Operators must separately confirm the upgrade of project-operated infrastructure and
   allow sufficient adoption time for other consuming clients.
5. Only after those checks may the operational hold be lifted. Proposal-key holders may then submit
   version-2 registrations and oracles may publish the corresponding network-data version `2` records.

The Bisq 1 batch RPC is renamed in step 2. During the bridge migration an upgraded oracle cannot call
the old batch endpoint and retains existing registrations under the specified fail-open transport
policy. Once the Bisq 1 update is enforced, batch revalidation resumes. If operators leave admission
enabled during this bounded interval, the operational hold among eligible proposal-key holders is the
required compatibility control; setting the operator override to `false` provides an additional
technical safeguard.

Registration protocol `2` prevents an ordinary attacker from registering a role: a valid request
requires both control of the Bisq 2 profile key and a signature from the accepted Bisq 1 proposal key
for the exact confirmed lockup. This does not provide a timed activation gate. Any legitimate or
compromised proposal-key holder satisfying those conditions can submit a valid request as soon as
oracle admission is enabled. Releasing the operational hold before minimum-version enforcement is
therefore safe only when operators have explicitly coordinated with all eligible role holders; an
operator override of `false` provides the stronger technical guarantee during migration.

Existing version-1 authorized-role records are not upgraded implicitly. An oracle republishes the
same network-data version and legacy hash shape, so older registrations continue to work normally
until they are cancelled or fail DAO revalidation. A version-2 registration is a new authorization;
it must not be relied upon by an older client which cannot verify its hash.

## Bisq 2 security-manager bans

Bridge verification and Bisq 2 security-manager bans are independent. Revalidation must never
republish or otherwise reactivate a role after a successful result; it only retains existing data or
removes invalid data.

A node applies a ban to the role which the ban names, also if the role arrives after the ban, for
example with an inventory response, or when an oracle node publishes it again.

The currently deployed ban payload names one exact `AuthorizedBondedRole` record. Consequently, a
new authorization created with another lockup/signature is a different record and is not
automatically covered by the old ban. Treating a ban as permanent revocation of the underlying DAO
proposal would require a separately specified stable ban identity and migration rule; it is not
silently introduced by this bridge protocol.

## User actions

The desktop registration form requires the proposal transaction id and lockup transaction id for a
new request and always emits protocol version `2`. Cancellation supports both forms:

- empty proposal and lockup ids cancel a known legacy version-1 registration;
- both ids present cancel the matching version-2 registration;
- supplying only one transaction id is invalid.

The bond user name and signature are required in both cases.

Cancellation is verified against the current DAO state before removal. If the bond has already left
its confirmed and unspent state, the cancellation request fails verification; DAO revalidation is
responsible for removing that invalid registration.

## Open points

- **One registration, several role records.** The identity of a registration leaves out the network ID and the
  addresses of a node. The oracle node still persists and publishes each request with other addresses as a separate
  registration and role record. The selection of a mediator counts each record, and a ban names one exact record, so
  a banned role can come back with other addresses. Rule 2 of [Sender of a registration](#sender-of-a-registration)
  limits the variants to the key of the profile, but not their addresses.
  - What would remove it: the oracle node keeps one record per registration identity. A new variant replaces the
    published record of the earlier one, and the oracle node publishes no variant of a banned record.
  - Decision needed: which identity a ban covers, and whether a new variant replaces the earlier record.
  - Who decides: the developers.
- **More than 1 000 registrations.** The batch revalidation sends at most 1 000 registrations to the Bisq 1 bridge.
  Above this number, the oracle node revalidates no registration.
  - Decision needed: a policy for more registrations, for example several batches of one DAO snapshot.
  - Who decides: the developers.
