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
public final class PayIDAccount extends Account<PayIDAccountPayload, FiatPaymentMethod> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.PAY_ID);

    public PayIDAccount(long creationDate, String accountName, PayIDAccountPayload accountPayload) {
        super(creationDate, accountName, PAYMENT_METHOD, accountPayload);
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean serializeForHash) {
        return getAccountBuilder(serializeForHash)
                .setPayIDAccount(toPayIDAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.PayIDAccount toPayIDAccountProto(boolean serializeForHash) {
        return resolveBuilder(getPayIDAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.PayIDAccount.Builder getPayIDAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.PayIDAccount.newBuilder();
    }

    public static PayIDAccount fromProto(bisq.account.protobuf.Account proto) {
        return new PayIDAccount(
                proto.getCreationDate(),
                proto.getAccountName(),
                PayIDAccountPayload.fromProto(proto.getAccountPayload())
        );
    }
}