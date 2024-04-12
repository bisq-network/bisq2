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

    public StrikeAccountPayload(String id, String paymentMethodName, String countryCode) {
        super(id, paymentMethodName, countryCode);
    }

    @Override
    public AccountPayload.Builder getBuilder(boolean ignoreAnnotation) {
        return getAccountPayloadBuilder(ignoreAnnotation)
                .setCountryBasedAccountPayload(
                        getCountryBasedAccountPayloadBuilder(ignoreAnnotation)
                                .setStrikeAccountPayload(bisq.account.protobuf.StrikeAccountPayload.newBuilder()
                                        .setHolderName("holderName")));
    }

    @Override
    public bisq.account.protobuf.AccountPayload toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
    }

    public static StrikeAccountPayload fromProto(AccountPayload proto) {
        return new StrikeAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                proto.getCountryBasedAccountPayload().getCountryCode());
    }
}
