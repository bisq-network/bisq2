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
public final class PixAccountPayload extends CountryBasedAccountPayload {

    private final String pixKey;

    public PixAccountPayload(String id, String paymentMethodName, String countryCode, String pixKey) {
        super(id, paymentMethodName, countryCode);
        this.pixKey = pixKey;
    }

    @Override
    public AccountPayload.Builder getBuilder(boolean ignoreAnnotation) {
        return getAccountPayloadBuilder(ignoreAnnotation).setCountryBasedAccountPayload(
                getCountryBasedAccountPayloadBuilder(ignoreAnnotation).setPixAccountPayload(
                                bisq.account.protobuf.PixAccountPayload.newBuilder()
                                        .setPixKey(pixKey)));
    }

    @Override
    public bisq.account.protobuf.AccountPayload toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
    }

    public static PixAccountPayload fromProto(AccountPayload proto) {
        bisq.account.protobuf.CountryBasedAccountPayload countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        bisq.account.protobuf.PixAccountPayload pixAccountPayload = countryBasedAccountPayload.getPixAccountPayload();
        return new PixAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                countryBasedAccountPayload.getCountryCode(),
                pixAccountPayload.getPixKey()
        );
    }
}
