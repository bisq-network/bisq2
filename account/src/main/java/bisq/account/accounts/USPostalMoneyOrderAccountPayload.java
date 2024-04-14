package bisq.account.accounts;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class USPostalMoneyOrderAccountPayload extends AccountPayload {

    private final String postalAddress;
    private final String holderName;

    public USPostalMoneyOrderAccountPayload(String id, String paymentMethodName, String postalAddress, String holderName) {
        super(id, paymentMethodName);
        this.postalAddress = postalAddress;
        this.holderName = holderName;
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setUsPostalMoneyOrderAccountPayload(toUSPostalMoneyOrderAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.USPostalMoneyOrderAccountPayload toUSPostalMoneyOrderAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getUSPostalMoneyOrderAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.USPostalMoneyOrderAccountPayload.Builder getUSPostalMoneyOrderAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.USPostalMoneyOrderAccountPayload.newBuilder()
                .setPostalAddress(postalAddress)
                .setHolderName(holderName);
    }

    public static USPostalMoneyOrderAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var usPostalOrderMoneyPayload = proto.getUsPostalMoneyOrderAccountPayload();
        return new USPostalMoneyOrderAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                usPostalOrderMoneyPayload.getPostalAddress(),
                usPostalOrderMoneyPayload.getHolderName()
        );
    }
}
