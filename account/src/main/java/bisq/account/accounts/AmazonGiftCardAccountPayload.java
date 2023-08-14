package bisq.account.accounts;

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

    public AmazonGiftCardAccountPayload(String id, String paymentMethodName,
                                        String countryCode, String emailOrMobileNr) {
        super(id, paymentMethodName, countryCode);
        this.emailOrMobileNr = emailOrMobileNr;
    }

    @Override
    public AccountPayload toProto() {
        return getAccountPayloadBuilder().setCountryBasedAccountPayload(
                        getCountryBasedAccountPayloadBuilder()
                                .setAmazonGiftCardAccountPayload(
                                        bisq.account.protobuf.AmazonGiftCardAccountPayload.newBuilder()
                                                .setEmailOrMobileNr(emailOrMobileNr)
                                ))
                .build();
    }

    public static AmazonGiftCardAccountPayload fromProto(AccountPayload proto) {
        bisq.account.protobuf.CountryBasedAccountPayload countryBasedAccountPayload =
                proto.getCountryBasedAccountPayload();
        bisq.account.protobuf.AmazonGiftCardAccountPayload amazonGiftCardAccountPayload =
                countryBasedAccountPayload.getAmazonGiftCardAccountPayload();
        return new AmazonGiftCardAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                countryBasedAccountPayload.getCountryCode(),
                amazonGiftCardAccountPayload.getEmailOrMobileNr()
        );
    }
}
