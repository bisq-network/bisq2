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
    public bisq.account.protobuf.AccountPayload toProto() {
        return getAccountPayloadBuilder()
                .setUsPostalMoneyOrderAccountPayload(bisq.account.protobuf.USPostalMoneyOrderAccountPayload.newBuilder()
                        .setPostalAddress(postalAddress)
                        .setHolderName(holderName))
                .build();
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
