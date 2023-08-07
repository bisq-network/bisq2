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
    public bisq.account.protobuf.Account toProto() {
        return getAccountBuilder()
                .setZelleAccount(bisq.account.protobuf.ZelleAccount.newBuilder())
                .build();
    }

    public static ZelleAccount fromProto(bisq.account.protobuf.Account proto) {
        return new ZelleAccount(
                proto.getCreationDate(),
                proto.getAccountName(),
                ZelleAccountPayload.fromProto(proto.getAccountPayload()));
    }
}
