# Maintaining Backward Compatibility

When introducing changes in new releases that alter network messages or persistence data, it is crucial to ensure these
changes are made in a backward-compatible manner, at least for the past release or until a forced update is required. We
have several tools at our disposal to achieve this. First, however, we need to understand the scenarios in which changes
can break backward compatibility.

## Protobuf

We use protobuf as the message format for both network messages and persistence data. Protobuf allows the addition of
new fields without breaking backward compatibility. An unupdated node receiving a message from an updated node with an
added field will ignore it. Conversely, an updated node receiving a message from an unupdated node will interpret the
missing value of the new field with the field type's default value. For more details, see
the [protobuf documentation on default values](https://protobuf.dev/programming-guides/proto3/#default).

Existing fields can be renamed, and certain other limited changes are allowed. For more information, refer to
the [protobuf programming guide](https://protobuf.dev/programming-guides/proto3/).

Since we use the serialized bytes of protobuf messages for several use cases, we need to ensure that changes in protobuf
definitions do not break these use cases. We are aware that protobuf does not guarantee deterministic encoding and
explicitly does not recommend it. Although developing our own serialization format was not feasible, we have never
encountered issues in Bisq 1. In case an updated version of the protobuf library results in different serialization, we
could stick with the previous version. Additionally, we do not need to support multiple languages consuming and
producing protobuf messages. If maps are used, it is important to use a `TreeMap`; otherwise, the order of the entries
is not deterministic. While using it only in Java would still work as a deterministically ordered map is used
internally, this is not true for other languages. For instance, Rust intentionally randomizes the order.

## Use Cases of Serialized Protobuf Message Data

- Using hash for signing authenticated or authorized data
- Using hash as a key in the storage hashmap
- Using hash as input for Proof of Work
- Using hash to ensure data integrity (`Contract`)
- Using hash for deterministic sorting in `Inventory`

Adding a backward compatibility field to a protobuf message would break the hash for various use cases, thus we need a
way to handle this.

## `@ExcludeForHash` Annotation

The `@ExcludeForHash` annotation is introduced to address these problems. Any fields in a class serialized via protobuf
can be annotated with `@ExcludeForHash`. If annotated, this field will be cleared (set to its default value) at
serialization. As fields with default values are not included in the wire format, it is the same as if they did not
exist. Adding a new field in a new version annotated with `@ExcludeForHash` will result in a wire format output
identical to the old version without that field.

For sending the message, we include the field, using the exclusion only for the use cases where it would otherwise break
the hash. The `serializeForHash()` method is used in that case, in contrast to the `serialize()` method, which does not
consider the annotation. As we need to traverse all our data structures, the client code for using protobuf got a bit
more restricted to follow an exact pattern with builders and passing the `serializeForHash` parameter down to the nested
classes to ensure that any annotated field in the object hierarchy is covered.

However, this alone was not enough to apply backward-compatible changes in reality. If we add a new field
like `applicationVersion` in `UserProfile` and annotate it with `@ExcludeForHash`, it would result in the exact same
hash. In that case, we wanted to replace already existing data with the new version, but that would not happen because
of the same hash (data storage ignores objects that are already present). If not replaced in the data storage, it also
will not get propagated in the gossip network. To solve that problem, we added an `excludeOnlyInVersions` parameter to
the annotation and introduced a version field (which is itself annotated with `@ExcludeForHash` if overridden in the
implementation class, with the default being `version` = 0).

This parameter, if set, takes a list of versions for which the `@ExcludeForHash` annotation should be applied. For other
versions, we ignore the annotation. This allows us more fine-grained control over how to apply changes.

### Case Studies

In the case of `UserProfile`, we added the parameter `@ExcludeForHash(excludeOnlyInVersions = {0})`
to `applicationVersion`. We also set `version` to 1. As it is annotated with `@ExcludeForHash`, it will be ignored in
any hashes. When we deploy the new version, the old nodes will not recognize the new `version` and `applicationVersion`
fields as added fields, and the hash will be the same as their values will be ignored at serialization for hash use
cases. A new node receiving messages from old nodes will set the version number to the default value (0)
and `applicationVersion` to an empty string. Messages sent between updated nodes will have `version` set to 1 and thus
do not apply the exclusion of `applicationVersion` for hash, thereby storing the new object and propagating it.

Other classes where we apply this new pattern for version 2.0.5 are:

- `AuthorizedBondedReputationData`
- `AuthorizedProofOfBurnData`
- `Capability`
- `InventoryResponse`

For `Capability` and `InventoryResponse`, we have a slightly different scenario. Here, we plan future changes in those
fields (e.g., adding new features without breaking the hash, thus excluding the `feature` field). We set the parameter
to some potential future version where we want to apply the
exclusion (`@ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})`). Again, `version` is set to 1 in the updated objects.
In upcoming versions, when we no longer need to support version 0 data, we can remove the parameter and have it excluded
for any version.

This will cause a problem when interacting between old and new nodes. For instance, `Capability` is part of the initial
connection establishment handshake protocol. The Proof of Work check would fail here as the serialized data of the old
node and the new node are different (the old node includes the `feature` field, while the new node does not).

To address this, we send those messages first with `version` set to 0. Thus, if the peer is an old node, the hash will
be the same so the handshake succeeds. If the peer is a new node, we get an exception, which will be caught, and then
the message is sent again with `version` set to 1.

For `InventoryResponse`, we have a slightly different situation. As the `inventory` data contains both old and new
versions of messages, if the sender of the `InventoryResponse` is an updated node, it would fail at the Proof of Work
check if the requester is an old node. Between two new nodes, all is good as both ignore the field. Between two old
nodes, it is good as well. If the requester is a new node and the `InventoryResponse` sender is an old node, it will be
okay as the `inventory` does not contain new data and the hashes are the same and not excluded. If the requester is an
old node and the `InventoryResponse` sender is a new node, it would fail.

To solve this, we filter out objects with `version > 0` if the requester has `version` set to 0 (we added a `version`
field to `InventoryRequest`). We also use the requester's `version` as the `version` in `InventoryResponse`. With these
changes, we can fix the above problem.

## Activation Date

Another tool for handling backward compatibility is setting an activation date for switching to a new feature.

## Forced Updates

We recently added support for enforcing a certain minimum version number for trading. We might consider doing this for
non-trading use cases as well, but the alert message itself may be enough to encourage users to update. Additionally,
certain changes will degrade network availability, providing "natural" pressure for users to update. We also added a
version distribution display to the network settings pane, which gives better insight into the version distribution of
active users. Since the application version is part of the UserProfile, which gets frequently republished for active
users, we get a pretty accurate picture.

## Testing

Regardless of the chosen strategy, it's important to test well and make sure it works as expected.
* A relative safe way is to copy the IDE project and run one node there and the other in the to-be-tested version.
   This avoids that changes are hotswapped to running apps. Also, it's best to run the seed on the old version to see if there are issues.
* Another option is to disable hotswap.
