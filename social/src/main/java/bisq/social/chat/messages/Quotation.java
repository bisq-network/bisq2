package bisq.social.chat.messages;

import bisq.common.proto.Proto;
import bisq.user.profile.NymLookup;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class Quotation implements Proto {
    private final String nym;
    private final String nickName;
    private final byte[] pubKeyHash;
    private final String message;

    public Quotation(String nym, String nickName, byte[] pubKeyHash, String message) {
        this.nym = nym;
        this.nickName = nickName;
        this.pubKeyHash = pubKeyHash;
        this.message = message;
    }

    public bisq.social.protobuf.Quotation toProto() {
        return bisq.social.protobuf.Quotation.newBuilder()
                .setNym(nym)
                .setNickName(nickName)
                .setPubKeyHash(ByteString.copyFrom(pubKeyHash))
                .setMessage(message)
                .build();
    }

    public static Quotation fromProto(bisq.social.protobuf.Quotation proto) {
        return new Quotation(proto.getNym(),
                proto.getNickName(),
                proto.getPubKeyHash().toByteArray(),
                proto.getMessage());
    }

    public String getUserName() {
        return NymLookup.getUserName(nym, nickName);
    }

    public boolean isValid() {
        return nym != null && !nym.isEmpty() &&
                nickName != null && !nickName.isEmpty() &&
                pubKeyHash != null && pubKeyHash.length > 0 &&
                message != null && !message.isEmpty();
    }
}
