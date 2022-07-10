package bisq.social.chat.messages;

import bisq.common.proto.Proto;
import bisq.security.pow.ProofOfWork;
import bisq.identity.profile.NymLookup;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class Quotation implements Proto {
    private final String nym;
    private final String nickName;
    private final ProofOfWork proofOfWork;
    private final String message;

    public Quotation(String nym, String nickName, ProofOfWork proofOfWork, String message) {
        this.nym = nym;
        this.nickName = nickName;
        this.proofOfWork = proofOfWork;
        this.message = message;
    }

    public bisq.social.protobuf.Quotation toProto() {
        return bisq.social.protobuf.Quotation.newBuilder()
                .setNym(nym)
                .setNickName(nickName)
                .setProofOfWork(proofOfWork.toProto())
                .setMessage(message)
                .build();
    }

    public static Quotation fromProto(bisq.social.protobuf.Quotation proto) {
        return new Quotation(proto.getNym(),
                proto.getNickName(),
                ProofOfWork.fromProto(proto.getProofOfWork()),
                proto.getMessage());
    }

    public String getUserName() {
        return NymLookup.getUserName(nym, nickName);
    }

    public boolean isValid() {
        return nym != null && !nym.isEmpty() &&
                nickName != null && !nickName.isEmpty() &&
                proofOfWork != null && proofOfWork.getSolution().length > 0 &&
                message != null && !message.isEmpty();
    }
}
