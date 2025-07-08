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
public final class PayIDAccountPayload extends AccountPayload<FiatPaymentMethod> {
    private final String holderName;
    private final String payId;

    public PayIDAccountPayload(String id, String holderName, String payId) {
        super(id);
        this.holderName = holderName;
        this.payId = payId;
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setPayIDAccountPayload(toPayIDAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.PayIDAccountPayload toPayIDAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getPayIDAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.PayIDAccountPayload.Builder getPayIDAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.PayIDAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setPayId(payId);
    }

    public static PayIDAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var payload = proto.getPayIDAccountPayload();
        return new PayIDAccountPayload(
                proto.getId(),
                payload.getHolderName(),
                payload.getPayId());
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.PAY_ID);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("user.paymentAccounts.holderName"), holderName,
                Res.get("user.paymentAccounts.payId.payId"), payId
        ).toString();
    }
}
