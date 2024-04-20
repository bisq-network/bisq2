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
public final class AmazonGiftCardAccount extends CountryBasedAccount<AmazonGiftCardAccountPayload, FiatPaymentMethod> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.AMAZON_GIFT_CARD);

    public AmazonGiftCardAccount(String accountName, AmazonGiftCardAccountPayload payload, Country country) {
        super(accountName, PAYMENT_METHOD, payload, country);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setAmazonGiftCardAccount(
                toAmazonGiftCardAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.AmazonGiftCardAccount toAmazonGiftCardAccountProto(boolean serializeForHash) {
        return resolveBuilder(getAmazonGiftCardAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.AmazonGiftCardAccount.Builder getAmazonGiftCardAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.AmazonGiftCardAccount.newBuilder();
    }

    @Override
    public Account.Builder getBuilder(boolean serializeForHash) {
        return getAccountBuilder(serializeForHash)
                .setCountryBasedAccount(getCountryBasedAccountBuilder(serializeForHash)
                        .setAmazonGiftCardAccount(bisq.account.protobuf.AmazonGiftCardAccount.newBuilder()));
    }

    public static AmazonGiftCardAccount fromProto(bisq.account.protobuf.Account proto) {
        return new AmazonGiftCardAccount(proto.getAccountName(),
                AmazonGiftCardAccountPayload.fromProto(proto.getAccountPayload()),
                Country.fromProto(proto.getCountryBasedAccount().getCountry()));
    }
}
