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
public final class PixAccount extends CountryBasedAccount<PixAccountPayload, FiatPaymentMethod> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.PIX);

    public PixAccount(String accountName, PixAccountPayload payload, Country country) {
        super(accountName, PAYMENT_METHOD, payload, country);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setPixAccount(
                toPixAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.PixAccount toPixAccountProto(boolean serializeForHash) {
        return resolveBuilder(getPixAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.PixAccount.Builder getPixAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.PixAccount.newBuilder();
    }

    public static PixAccount fromProto(bisq.account.protobuf.Account proto) {
        return new PixAccount(proto.getAccountName(),
                PixAccountPayload.fromProto(proto.getAccountPayload()),
                Country.fromProto(proto.getCountryBasedAccount().getCountry()));
    }
}
