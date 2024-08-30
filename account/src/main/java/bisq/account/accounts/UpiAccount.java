package bisq.account.accounts;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.protobuf.Account;
import bisq.common.locale.Country;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class UpiAccount extends CountryBasedAccount<UpiAccountPayload, FiatPaymentMethod> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.UPI);

    public UpiAccount(String accountName, UpiAccountPayload accountPayload, Country country) {
        super(accountName, PAYMENT_METHOD, accountPayload, country);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setUpiAccount(
                toUpiAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.UpiAccount toUpiAccountProto(boolean serializeForHash) {
        return resolveBuilder(getUpiAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.UpiAccount.Builder getUpiAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.UpiAccount.newBuilder();
    }

    public static UpiAccount fromProto(Account proto) {
        return new UpiAccount(
                proto.getAccountName(),
                UpiAccountPayload.fromProto(proto.getAccountPayload()),
                Country.fromProto(proto.getCountryBasedAccount().getCountry()));
    }
}
