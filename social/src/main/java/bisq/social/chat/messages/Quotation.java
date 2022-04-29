package bisq.social.chat.messages;

import bisq.common.data.ByteArray;
import bisq.common.proto.Proto;
import bisq.social.user.NymLookup;

public record Quotation(String nym, String nickName, ByteArray pubKeyHash, String message) implements Proto {
    public bisq.social.protobuf.Quotation toProto() {
        return bisq.social.protobuf.Quotation.newBuilder()
                .setNym(nym)
                .setNickName(nickName)
                .setPubKeyHash(pubKeyHash.toProto())
                .setMessage(message)
                .build();
    }

    public static Quotation fromProto(bisq.social.protobuf.Quotation proto) {
        return new Quotation(proto.getNym(),
                proto.getNickName(),
                ByteArray.fromProto(proto.getPubKeyHash()),
                proto.getMessage());
    }

    public String getUserName() {
        return NymLookup.getUserName(nym, nickName);
    }
}
