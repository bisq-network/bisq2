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
public final class StrikeAccountPayload extends CountryBasedAccountPayload {
    private final String holderName;

    public StrikeAccountPayload(String id, String paymentMethodName, String countryCode, String holderName) {
        super(id, paymentMethodName, countryCode);
        this.holderName = holderName;
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setStrikeAccountPayload(
                toStrikeAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.StrikeAccountPayload toStrikeAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getStrikeAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.StrikeAccountPayload.Builder getStrikeAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.StrikeAccountPayload.newBuilder().setHolderName(holderName);
    }

    public static StrikeAccountPayload fromProto(AccountPayload proto) {
        return new StrikeAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                proto.getCountryBasedAccountPayload().getCountryCode(),
                proto.getCountryBasedAccountPayload().getStrikeAccountPayload().getHolderName());
    }
}
