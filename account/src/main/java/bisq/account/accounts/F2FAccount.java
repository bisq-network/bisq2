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
public final class F2FAccount extends CountryBasedAccount<F2FAccountPayload, FiatPaymentMethod> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.F2F);

    public F2FAccount(String accountName, F2FAccountPayload payload, Country country) {
        super(accountName, PAYMENT_METHOD, payload, country);
    }

    @Override
    public bisq.account.protobuf.Account toProto() {
        return getAccountBuilder()
                .setCountryBasedAccount(getCountryBasedAccountBuilder()
                        .setF2FAccount(bisq.account.protobuf.F2FAccount.newBuilder()))
                .build();
    }

    public static F2FAccount fromProto(bisq.account.protobuf.Account proto) {
        return new F2FAccount(proto.getAccountName(),
                F2FAccountPayload.fromProto(proto.getAccountPayload()),
                Country.fromProto(proto.getCountryBasedAccount().getCountry()));
    }
}
