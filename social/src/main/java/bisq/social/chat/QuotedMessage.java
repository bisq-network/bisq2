package bisq.social.chat;

import bisq.common.data.ByteArray;
import bisq.common.proto.Proto;

public record QuotedMessage(String userName, ByteArray pubKeyHash, String message) implements Proto {
    public bisq.social.protobuf.QuotedMessage toProto() {
        return bisq.social.protobuf.QuotedMessage.newBuilder()
                .setUserName(userName)
                .setPubKeyHash(pubKeyHash.toProto())
                .setMessage(message)
                .build();
    }

    public static QuotedMessage fromProto(bisq.social.protobuf.QuotedMessage proto) {
        return new QuotedMessage(proto.getUserName(),
                ByteArray.fromProto(proto.getPubKeyHash()),
                proto.getMessage());
    }
}
