package bisq.account.accounts;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.protobuf.AccountPayload;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class AmazonGiftCardAccountPayload extends CountryBasedAccountPayload {
    private final String emailOrMobileNr;

    public AmazonGiftCardAccountPayload(String id, String countryCode, String emailOrMobileNr) {
        super(id, countryCode);
        this.emailOrMobileNr = emailOrMobileNr;
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setAmazonGiftCardAccountPayload(
                toAmazonGiftCardAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.AmazonGiftCardAccountPayload toAmazonGiftCardAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getAmazonGiftCardAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.AmazonGiftCardAccountPayload.Builder getAmazonGiftCardAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.AmazonGiftCardAccountPayload.newBuilder().setEmailOrMobileNr(emailOrMobileNr);
    }

    public static AmazonGiftCardAccountPayload fromProto(AccountPayload proto) {
        bisq.account.protobuf.CountryBasedAccountPayload countryBasedAccountPayload =
                proto.getCountryBasedAccountPayload();
        bisq.account.protobuf.AmazonGiftCardAccountPayload amazonGiftCardAccountPayload =
                countryBasedAccountPayload.getAmazonGiftCardAccountPayload();
        return new AmazonGiftCardAccountPayload(
                proto.getId(),
                countryBasedAccountPayload.getCountryCode(),
                amazonGiftCardAccountPayload.getEmailOrMobileNr()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.AMAZON_GIFT_CARD);
    }
}
