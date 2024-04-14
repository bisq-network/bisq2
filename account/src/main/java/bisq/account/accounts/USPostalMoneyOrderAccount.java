package bisq.account.accounts;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class USPostalMoneyOrderAccount extends Account<USPostalMoneyOrderAccountPayload, FiatPaymentMethod> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.US_POSTAL_MONEY_ORDER);

    public USPostalMoneyOrderAccount(long creationDate, String accountName, USPostalMoneyOrderAccountPayload accountPayload) {
        super(creationDate, accountName, PAYMENT_METHOD, accountPayload);
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean serializeForHash) {
        return getAccountBuilder(serializeForHash)
                .setUsPostalMoneyOrderAccount(toUSPostalMoneyOrderAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.USPostalMoneyOrderAccount toUSPostalMoneyOrderAccountProto(boolean serializeForHash) {
        return resolveBuilder(getUSPostalMoneyOrderAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.USPostalMoneyOrderAccount.Builder getUSPostalMoneyOrderAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.USPostalMoneyOrderAccount.newBuilder();
    }

    public static USPostalMoneyOrderAccount fromProto(bisq.account.protobuf.Account proto) {
        return new USPostalMoneyOrderAccount(
                proto.getCreationDate(),
                proto.getAccountName(),
                USPostalMoneyOrderAccountPayload.fromProto(proto.getAccountPayload()));
    }
}