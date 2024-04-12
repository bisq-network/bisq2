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
public final class UpiAccountPayload extends CountryBasedAccountPayload {

    private final String virtualPaymentAddress;

    public UpiAccountPayload(String id, String paymentMethodName, String countryCode, String virtualPaymentAddress) {
        super(id, paymentMethodName, countryCode);
        this.virtualPaymentAddress = virtualPaymentAddress;
    }

    @Override
    public AccountPayload.Builder getBuilder(boolean ignoreAnnotation) {
        return getAccountPayloadBuilder(ignoreAnnotation)
                .setCountryBasedAccountPayload(
                        getCountryBasedAccountPayloadBuilder(ignoreAnnotation)
                                .setUpiAccountPayload(bisq.account.protobuf.UpiAccountPayload.newBuilder()
                                        .setVirtualPaymentAddress("virtualPaymentAddress")));
    }

    @Override
    public bisq.account.protobuf.AccountPayload toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
    }

    public static UpiAccountPayload fromProto(AccountPayload proto) {
        var countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        return new UpiAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                countryBasedAccountPayload.getCountryCode(),
                countryBasedAccountPayload.getUpiAccountPayload().getVirtualPaymentAddress());
    }
}
