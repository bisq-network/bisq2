package bisq.account.accounts;

import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class InteracETransferAccountPayload extends AccountPayload<FiatPaymentMethod> {
    private final String holderName;
    private final String email;
    private final String question;
    private final String answer;

    public InteracETransferAccountPayload(String id, String holderName, String email, String question, String answer) {
        super(id);
        this.holderName = holderName;
        this.email = email;
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
                .setHolderName(holderName)
                .setEmail(email)
                .setQuestion(question)
                .setAnswer(answer);
    }

    public static InteracETransferAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var interactETransferAccountPayload = proto.getInteracETransferAccountPayload();
        return new InteracETransferAccountPayload(
                proto.getId(),
                interactETransferAccountPayload.getHolderName(),
                interactETransferAccountPayload.getEmail(),
                interactETransferAccountPayload.getQuestion(),
                interactETransferAccountPayload.getAnswer()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.INTERAC_E_TRANSFER);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("user.paymentAccounts.holderName"), holderName,
                Res.get("user.paymentAccounts.email"), email,
                Res.get("user.paymentAccounts.interacETransfer.question"), question,
                Res.get("user.paymentAccounts.interacETransfer.answer"), answer
        ).toString();
    }
}
