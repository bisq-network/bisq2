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
    public bisq.account.protobuf.Account.Builder getBuilder(boolean serializeForHash) {
        return getAccountBuilder(serializeForHash)
                .setCashByMailAccount(toCashByMailAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.CashByMailAccount toCashByMailAccountProto(boolean serializeForHash) {
        return resolveBuilder(getCashByMailAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.CashByMailAccount.Builder getCashByMailAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.CashByMailAccount.newBuilder();
    }

    public static CashByMailAccount fromProto(bisq.account.protobuf.Account proto) {
        return new CashByMailAccount(
                proto.getCreationDate(),
                proto.getAccountName(),
                CashByMailAccountPayload.fromProto(proto.getAccountPayload())
        );
    }
}
