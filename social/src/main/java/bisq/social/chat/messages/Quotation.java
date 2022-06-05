package bisq.social.chat.messages;

import bisq.common.proto.Proto;
import bisq.security.pow.ProofOfWork;
import bisq.social.user.NymLookup;

public record Quotation(String nym, String nickName, ProofOfWork proofOfWork, String message) implements Proto {
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
}
