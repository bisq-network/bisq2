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
public final class StrikeAccount extends CountryBasedAccount<StrikeAccountPayload, FiatPaymentMethod> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.STRIKE);

    public StrikeAccount(String accountName, StrikeAccountPayload accountPayload, Country country) {
        super(accountName, PAYMENT_METHOD, accountPayload, country);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setStrikeAccount(
                toStrikeAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.StrikeAccount toStrikeAccountProto(boolean serializeForHash) {
        return resolveBuilder(getStrikeAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.StrikeAccount.Builder getStrikeAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.StrikeAccount.newBuilder();
    }

    public static StrikeAccount fromProto(Account proto) {
        return new StrikeAccount(
                proto.getAccountName(),
                StrikeAccountPayload.fromProto(proto.getAccountPayload()),
                Country.fromProto(proto.getCountryBasedAccount().getCountry()));
    }
}
