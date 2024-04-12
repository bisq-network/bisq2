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
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean ignoreAnnotation) {
        return getAccountPayloadBuilder(ignoreAnnotation)
                .setInteracETransferAccountPayload(
                        bisq.account.protobuf.InteracETransferAccountPayload.newBuilder()
                                .setEmail(email)
                                .setHolderName(holderName)
                                .setQuestion(question)
                                .setAnswer(answer)
                );
    }

    @Override
    public bisq.account.protobuf.AccountPayload toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
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
