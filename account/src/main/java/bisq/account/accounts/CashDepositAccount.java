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
    public bisq.account.protobuf.Account.Builder getBuilder(boolean ignoreAnnotation) {
        return getAccountBuilder(ignoreAnnotation)
                .setCountryBasedAccount(getCountryBasedAccountBuilder(ignoreAnnotation)
                        .setBankAccount(getBankAccountBuilder(ignoreAnnotation)
                                .setCashDepositAccount(bisq.account.protobuf.CashDepositAccount.newBuilder())));
    }

    @Override
    public bisq.account.protobuf.Account toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
    }

    public static CashDepositAccount fromProto(bisq.account.protobuf.Account proto) {
        return new CashDepositAccount(
                proto.getAccountName(),
                CashDepositAccountPayload.fromProto(proto.getAccountPayload()),
                Country.fromProto(proto.getCountryBasedAccount().getCountry()));
    }
}
