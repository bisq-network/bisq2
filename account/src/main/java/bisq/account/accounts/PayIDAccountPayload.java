package bisq.account.accounts;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class PayIDAccountPayload extends AccountPayload {

    private final String bankAccountName;
    private final String payID;

    public PayIDAccountPayload(String id, String paymentMethodName, String bankAccountName, String payID) {
        super(id, paymentMethodName);
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
                proto.getPaymentMethodName(),
                payload.getBankAccountName(),
                payload.getPayId());
    }
}
