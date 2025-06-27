package bisq.account.accounts;

import bisq.account.payment_method.FiatPaymentMethod;
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

    public PixAccountPayload(String id, String countryCode, String pixKey) {
        super(id, countryCode);
        this.pixKey = pixKey;
    }
    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setPixAccountPayload(
                toPixAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.PixAccountPayload toPixAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getPixAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.PixAccountPayload.Builder getPixAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.PixAccountPayload.newBuilder().setPixKey(pixKey);
    }

    public static PixAccountPayload fromProto(AccountPayload proto) {
        bisq.account.protobuf.CountryBasedAccountPayload countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        bisq.account.protobuf.PixAccountPayload pixAccountPayload = countryBasedAccountPayload.getPixAccountPayload();
        return new PixAccountPayload(
                proto.getId(),
                countryBasedAccountPayload.getCountryCode(),
                pixAccountPayload.getPixKey()
        );
    }
}
