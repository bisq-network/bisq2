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
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setUpiAccountPayload(
                toUpiAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.UpiAccountPayload toUpiAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getUpiAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.UpiAccountPayload.Builder getUpiAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.UpiAccountPayload.newBuilder().setVirtualPaymentAddress(virtualPaymentAddress);
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
