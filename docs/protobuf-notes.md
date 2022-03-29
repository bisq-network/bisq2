## Usage of protobuf in Bisq

This document gives an overview about some conventions and reasoning behind the way how we integrated protobuf.

### Modular approach

We use a protobuf file per module to avoid getting too large protobuf definitions and to fit into the java module
design. We use following conventions:

Protobuf package: `[module name]`
Java package name: `bisq.[module name].protobuf`
E.g. for the `security` module it is:

```
package security;
option java_package = "bisq.security.protobuf";
```

### Dealing with dependencies

To use a protobuf definition from another module where our current module has a dependency to (e.g. security module used
by network module) we use the import statement in protobuf and reference the external protobuf type with the module
name. E.g.

```
import "security.proto";

message NetworkId {
  repeated AddressTransportTypeTuple addressNetworkTypeTuple = 1;
  security.PubKey pubKey = 2;
  string nodeId = 3;
}
```

IntelliJ IDE does not fully recognize the imports but for compile all works fine.

In case we need to deserialize a protobuf binary input to the java object, but we do not have the dependencies we
use [Any](https://developers.google.com/protocol-buffers/docs/proto3#any) which represents the protobuf data as binary
blob. It requires importing the `Any` definition:

```
import "google/protobuf/any.proto";

message ErrorStatus {
  string message = 1;
  repeated google.protobuf.Any details = 2;
}
```

### Resolver concept

We need this in the `network` module. At deserializing we do not have the Java type and need a way to resolve the binary
data by provided a resolver implementation which we get provided by the application. The `network` module only knows the
interface to the resolver.

The `Any` object provides a field for the type of the protobuf definition as string. It has the format:

```
type.googleapis.com/[packagename].[messagename]
```

This helps us to figure out which module (`[packagename]`) and which message (`[messagename]`) is the Java type for
converting the protobuf binary to a Java object. We use that string as key in a map holding the different resolvers.

### Data types, Interface

The base classes for all Bisq classes supporting protobuf is `Proto` and `ProtoEnum` in case of enums.
[Enums](https://developers.google.com/protocol-buffers/docs/proto3#enum) in protobuf have a few limitations. For
instance multiple enums at the same level must have unique enum entries.

`ProtoResolver` and `ProtoResolverMap` are used for dealing with the deserialization issues for unknown types as
mentioned above. Only classes which are not in the dependency graph of the `network` module are required to provide a
resolver, and it is only required for the outer layers.  
There are 3 types of those outer layers:

- `NetworkMessage` (e.g. `PrivateChatMessage` or a trade protocol message)
- `DistributedData` (e.g. `Offer` or `PublicChatMessage`)
- `PersistableStore` (e.g. `KeyPairStore`)

Mailbox messages require support of both `NetworkMessage` and `DistributedData` as in case the message does not arrive
as direct message it will be stored in the network as `DistributedData`.

`NetworkMessage` is a field in the `NetworkEnvelope` which is the outer object when sending protobuf messages over the
wire.
`DistributedData` is a field inside `AuthenticatedData` which represents the data we distribute to the p2p network
storage. At the data storage we use the name of the `DistributedData` class as name for the storage file (
e.g. `Bisq/db/network/AuthenticatedDataStore/PublicChatMessage`)
`PersistableStore` is the base class for all persisted data store classes. There can be multiple per module.

We pack externally defined `NetworkMessage` object inside the `ExternalNetworkMessage` message as an `Any` type.

`PersistableStore` is handled in the `persistence` module. The `getOrCreatePersistence` method which does the setup for
the persistence framework is handling also the registration of the resolvers internally. So devs do not need to do the
resolver registration.

For the `NetworkMessage` and `DistributedData` implementations we need to register the resolvers at startup before any
protobuf code gets executed. We do that in the constructor of the `ServiceProvider`
implementations (`DefaultApplicationService` and `NetworkApplicationService`).

```
// Register resolvers for distributedData 
DistributedDataResolver.addResolver("social.ChatMessage", PublicChatMessage.getResolver());
DistributedDataResolver.addResolver("offer.Offer", Offer.getResolver());

// Register resolvers for networkMessages 
NetworkMessageResolver.addResolver("social.ChatMessage", PrivateChatMessage.getResolver());
```

This solution is not really great but so far I have not found a better way. To do it in the domain services might be an
option but the seedNode application does not use those domains, so it would be weird to instantiate a `OfferService` if
not used. But the seedNode still requires the code dependency and the registration of the resolver as an offer is stored
in the dataStore of the network module and it needs to be able to deserialize it.

### Dealing with missing inheritance support in protobuf

Protobuf does not support inheritance. To simulate that to get a compatible version to the Java code we use
the [oneof](https://developers.google.com/protocol-buffers/docs/proto3#oneof) feature. It lets one define the existing (
known) subclasses inside the base class. E.g.:

```
message PrivateChannel {
  ChatUser peer = 1;
  UserProfile myProfile = 2;
}
message PublicChannel {
  string channelName = 1;
  string description = 2;
  ChatUser channelAdmin = 3;
  repeated ChatUser channelModerators = 4;
}
message Channel {
  string id = 1;
  NotificationSetting notificationSetting = 2;
  repeated ChatMessage chatMessages = 3;
  oneof message{
    PrivateChannel privateChannel = 10;
    PublicChannel publicChannel = 11;
  }
}
```

We leave a gap in the numbers between the field definitions and the `oneof` so when fields gets added that the numbers
are still in a logical order.

To resolve at runtime which subclass should be used for deserialization we use a switch with the messageCase and
delegate the deserialization (`fromProto`) to the subclass:

```
public static Channel<? extends ChatMessage> fromProto(bisq.social.protobuf.Channel proto) {
        switch (proto.getMessageCase()) {
            case PRIVATECHANNEL -> {
                return PrivateChannel.fromProto(proto, proto.getPrivateChannel());
            }
            case PUBLICCHANNEL -> {
                return PublicChannel.fromProto(proto, proto.getPublicChannel());
            }
        }
    }
```

### Maps in protobuf

[Maps](https://developers.google.com/protocol-buffers/docs/proto3#maps) in protobuf have some limitations. If we need a
data type as key which is not supported (e.g. `ByteArray`) we mimic a map with a list of `MapEntry` messages. As order
in a map is not deterministic we must not use maps in use cases where we require deterministic behaviour like in any
class used inside `DistributedData`. For those cases we also require that lists are deterministically sorted.

### Code conventions

Official code convention is to use underscore and not camelCase inside protobuf files. In Bisq1 we followed that, but it
did not materialize in any benefit but made it more error-prone and cumbersome when converting java code to protobuf as
well as made search harder. I prefer to drop that official convention.

When defining the message types we should follow the dependency order how the messages are composed. E.g. low level
messages are on top.

If a field in Java is optional we should use th optional specifies as well in the protobuf file.

The `PersistableStore` implementations should be owned and created by a service class which is also responsible for the
`persist()` calls. the `PersistableStore` can be used as data model container and observeAble wrappers can be used.

The protobuf methods should be placed after the constructor(s). If there is a common base class this class provides a
method for the builder of the base class. E.g.

```
 public bisq.social.protobuf.Channel.Builder getChannelBuilder() {
        return bisq.social.protobuf.Channel.newBuilder()
                .setId(id)
                .setNotificationSetting(notificationSetting.get().toProto())
                .addAllChatMessages(chatMessages.stream().map(this::getChatMessageProto).collect(Collectors.toList()));
    }
```
The concrete class use that builder to set its own builder and adds its field if there are any.
```
 public bisq.social.protobuf.ChatMessage toProto() {
        return getChatMessageBuilder().setPublicChatMessage(bisq.social.protobuf.PublicChatMessage.newBuilder()).build();
    }
```