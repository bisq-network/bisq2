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
public final class PixAccount extends CountryBasedAccount<PixAccountPayload, FiatPaymentMethod> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.PIX);

    public PixAccount(String accountName, PixAccountPayload payload, Country country) {
        super(accountName, PAYMENT_METHOD, payload, country);
    }

    @Override
    public Account toProto() {
        return getAccountBuilder()
                .setCountryBasedAccount(getCountryBasedAccountBuilder()
                        .setPixAccount(bisq.account.protobuf.PixAccount.newBuilder()))
                .build();
    }

    public static PixAccount fromProto(bisq.account.protobuf.Account proto) {
        return new PixAccount(proto.getAccountName(),
                PixAccountPayload.fromProto(proto.getAccountPayload()),
                Country.fromProto(proto.getCountryBasedAccount().getCountry()));
    }
}
