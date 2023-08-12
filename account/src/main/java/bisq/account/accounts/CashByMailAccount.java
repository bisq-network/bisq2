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
public final class CashByMailAccount extends Account<CashByMailAccountPayload, FiatPaymentMethod> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.CASH_BY_MAIL);

    public CashByMailAccount(long creationDate, String accountName, CashByMailAccountPayload accountPayload) {
        super(creationDate, accountName, PAYMENT_METHOD, accountPayload);
    }

    @Override
    public bisq.account.protobuf.Account toProto() {
        return getAccountBuilder()
                .setCashByMailAccount(bisq.account.protobuf.CashByMailAccount.newBuilder())
                .build();
    }

    public static CashByMailAccount fromProto(bisq.account.protobuf.Account proto) {
        return new CashByMailAccount(
                proto.getCreationDate(),
                proto.getAccountName(),
                CashByMailAccountPayload.fromProto(proto.getAccountPayload())
        );
    }
}
