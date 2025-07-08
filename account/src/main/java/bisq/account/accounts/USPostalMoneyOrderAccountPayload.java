package bisq.account.accounts;

import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class USPostalMoneyOrderAccountPayload extends AccountPayload<FiatPaymentMethod> {
    private final String holderName;
    private final String postalAddress;

    public USPostalMoneyOrderAccountPayload(String id, String holderName, String postalAddress) {
        super(id);
        this.holderName = holderName;
        this.postalAddress = postalAddress;
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
                .setHolderName(holderName)
                .setPostalAddress(postalAddress);
    }

    public static USPostalMoneyOrderAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var usPostalOrderMoneyPayload = proto.getUsPostalMoneyOrderAccountPayload();
        return new USPostalMoneyOrderAccountPayload(
                proto.getId(),
                usPostalOrderMoneyPayload.getHolderName(),
                usPostalOrderMoneyPayload.getPostalAddress()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.US_POSTAL_MONEY_ORDER);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("user.paymentAccounts.holderName"), holderName,
                Res.get("user.paymentAccounts.postalAddress"), postalAddress
        ).toString();
    }
}
