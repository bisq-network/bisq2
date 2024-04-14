package bisq.account.accounts;

import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class FasterPaymentsAccountPayload extends AccountPayload {

    private final String sortCode;
    private final String accountNr;

    public FasterPaymentsAccountPayload(String id, String paymentMethodName, String sortCode, String accountNr) {
        super(id, paymentMethodName);
        this.sortCode = sortCode;
        this.accountNr = accountNr;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateText(sortCode, 50);
        NetworkDataValidation.validateText(accountNr, 50);
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setFasterPaymentsAccountPayload(toFasterPaymentsAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.FasterPaymentsAccountPayload toFasterPaymentsAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getFasterPaymentsAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.FasterPaymentsAccountPayload.Builder getFasterPaymentsAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.FasterPaymentsAccountPayload.newBuilder()
                .setSortCode(sortCode)
                .setAccountNr(accountNr);
    }

    public static FasterPaymentsAccountPayload fromProto(bisq.account.protobuf.AccountPayload account) {
        var fasterPaymentsPayload = account.getFasterPaymentsAccountPayload();
        return new FasterPaymentsAccountPayload(
                account.getId(),
                account.getPaymentMethodName(),
                fasterPaymentsPayload.getSortCode(),
                fasterPaymentsPayload.getAccountNr());
    }
}
