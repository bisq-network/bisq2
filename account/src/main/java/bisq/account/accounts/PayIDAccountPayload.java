package bisq.account.accounts;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class PayIDAccountPayload extends AccountPayload<FiatPaymentMethod> {
    private final String bankAccountName;
    private final String payID;

    public PayIDAccountPayload(String id, String bankAccountName, String payID) {
        super(id);
        this.bankAccountName = bankAccountName;
        this.payID = payID;
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
                .setBankAccountName(bankAccountName)
                .setPayId(payID);
    }

    public static PayIDAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var payload = proto.getPayIDAccountPayload();
        return new PayIDAccountPayload(
                proto.getId(),
                payload.getBankAccountName(),
                payload.getPayId());
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.PAY_ID);
    }
}
