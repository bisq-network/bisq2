package bisq.account.accounts;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class InteracETransferAccountPayload extends AccountPayload {

    private final String email;
    private final String holderName;
    private final String question;
    private final String answer;

    public InteracETransferAccountPayload(String id, String paymentMethodName, String email, String holderName, String question, String answer) {
        super(id, paymentMethodName);
        this.email = email;
        this.holderName = holderName;
        this.question = question;
        this.answer = answer;
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setInteracETransferAccountPayload(toInteracETransferAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.InteracETransferAccountPayload toInteracETransferAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getInteracETransferAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.InteracETransferAccountPayload.Builder getInteracETransferAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.InteracETransferAccountPayload.newBuilder()
                .setEmail(email)
                .setHolderName(holderName)
                .setQuestion(question)
                .setAnswer(answer);
    }

    public static InteracETransferAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var interactETransferAccountPayload = proto.getInteracETransferAccountPayload();
        return new InteracETransferAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                interactETransferAccountPayload.getEmail(),
                interactETransferAccountPayload.getHolderName(),
                interactETransferAccountPayload.getQuestion(),
                interactETransferAccountPayload.getAnswer()
        );
    }
}
