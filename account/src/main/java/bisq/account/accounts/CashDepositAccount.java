package bisq.account.accounts;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.common.locale.Country;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class CashDepositAccount extends BankAccount<CashDepositAccountPayload> {
    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.CASH_DEPOSIT);

    public CashDepositAccount(String accountName, CashDepositAccountPayload payload, Country country) {
        super(accountName, PAYMENT_METHOD, payload, country);
    }

    @Override
    protected bisq.account.protobuf.BankAccount.Builder getBankAccountBuilder(boolean serializeForHash) {
        return super.getBankAccountBuilder(serializeForHash).setCashDepositAccount(
                toCashDepositAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.CashDepositAccount toCashDepositAccountProto(boolean serializeForHash) {
        return resolveBuilder(getCashDepositAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.CashDepositAccount.Builder getCashDepositAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.CashDepositAccount.newBuilder();
    }

    public static CashDepositAccount fromProto(bisq.account.protobuf.Account proto) {
        return new CashDepositAccount(
                proto.getAccountName(),
                CashDepositAccountPayload.fromProto(proto.getAccountPayload()),
                Country.fromProto(proto.getCountryBasedAccount().getCountry()));
    }
}
