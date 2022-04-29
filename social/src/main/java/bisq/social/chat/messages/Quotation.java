package bisq.social.chat.messages;

import bisq.common.data.ByteArray;
import bisq.common.proto.Proto;
import bisq.social.user.NickNameLookup;

public record Quotation(String profileId, String nickName, ByteArray pubKeyHash, String message) implements Proto {
    public bisq.social.protobuf.QuotedMessage toProto() {
        return bisq.social.protobuf.QuotedMessage.newBuilder()
                .setProfileId(profileId)
                .setNickName(nickName)
                .setPubKeyHash(pubKeyHash.toProto())
                .setMessage(message)
                .build();
    }

    public static Quotation fromProto(bisq.social.protobuf.QuotedMessage proto) {
        return new Quotation(proto.getProfileId(),
                proto.getNickName(),
                ByteArray.fromProto(proto.getPubKeyHash()),
                proto.getMessage());
    }

    public String getUserName() {
        return NickNameLookup.getUserName(profileId, nickName);
    }
}
