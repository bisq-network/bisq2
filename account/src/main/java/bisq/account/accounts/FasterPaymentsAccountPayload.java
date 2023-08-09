package bisq.account.accounts;

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
    }

    @Override
    public bisq.account.protobuf.AccountPayload toProto() {
        return getAccountPayloadBuilder()
                .setFasterPaymentsAccountPayload(bisq.account.protobuf.FasterPaymentsAccountPayload.newBuilder()
                        .setSortCode(sortCode)
                        .setAccountNr(accountNr))
                .build();
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
