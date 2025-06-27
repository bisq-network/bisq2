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
public final class FasterPaymentsAccount extends Account<FiatPaymentMethod, FasterPaymentsAccountPayload> {
    public FasterPaymentsAccount(long creationDate, String accountName, FasterPaymentsAccountPayload accountPayload) {
        super(creationDate, accountName, accountPayload);
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean serializeForHash) {
        return getAccountBuilder(serializeForHash)
                .setFasterPaymentsAccount(toFasterPaymentsAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.FasterPaymentsAccount toFasterPaymentsAccountProto(boolean serializeForHash) {
        return resolveBuilder(getFasterPaymentsAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.FasterPaymentsAccount.Builder getFasterPaymentsAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.FasterPaymentsAccount.newBuilder();
    }

    public static FasterPaymentsAccount fromProto(bisq.account.protobuf.Account proto) {
        return new FasterPaymentsAccount(
                proto.getCreationDate(),
                proto.getAccountName(),
                FasterPaymentsAccountPayload.fromProto(proto.getAccountPayload()));
    }
}
