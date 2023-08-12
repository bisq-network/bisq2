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
    public bisq.account.protobuf.AccountPayload toProto() {
        return getAccountPayloadBuilder()
                .setPayIDAccountPayload(bisq.account.protobuf.PayIDAccountPayload.newBuilder()
                        .setBankAccountName(bankAccountName)
                        .setPayId(payID))
                .build();
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
