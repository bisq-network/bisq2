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
public class ZelleAccount extends Account<ZelleAccountPayload, FiatPaymentMethod> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ZELLE);

    public ZelleAccount(long creationDate, String accountName, ZelleAccountPayload accountPayload) {
        super(creationDate, accountName, PAYMENT_METHOD, accountPayload);
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean serializeForHash) {
        return getAccountBuilder(serializeForHash)
                .setZelleAccount(toZelleAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.ZelleAccount toZelleAccountProto(boolean serializeForHash) {
        return resolveBuilder(getZelleAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.ZelleAccount.Builder getZelleAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.ZelleAccount.newBuilder();
    }

    public static ZelleAccount fromProto(bisq.account.protobuf.Account proto) {
        return new ZelleAccount(
                proto.getCreationDate(),
                proto.getAccountName(),
                ZelleAccountPayload.fromProto(proto.getAccountPayload()));
    }
}
