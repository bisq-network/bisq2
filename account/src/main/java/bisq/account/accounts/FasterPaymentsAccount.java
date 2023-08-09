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
public final class FasterPaymentsAccount extends Account<FasterPaymentsAccountPayload, FiatPaymentMethod> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.FASTER_PAYMENTS);


    public FasterPaymentsAccount(long creationDate, String accountName, FasterPaymentsAccountPayload accountPayload) {
        super(creationDate, accountName, PAYMENT_METHOD, accountPayload);
    }

    @Override
    public bisq.account.protobuf.Account toProto() {
        return getAccountBuilder()
                .setFasterPaymentsAccount(bisq.account.protobuf.FasterPaymentsAccount.newBuilder())
                .build();
    }

    public static FasterPaymentsAccount fromProto(bisq.account.protobuf.Account proto) {
        return new FasterPaymentsAccount(
                proto.getCreationDate(),
                proto.getAccountName(),
                FasterPaymentsAccountPayload.fromProto(proto.getAccountPayload()));
    }
}
