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
public final class InteracETransferAccount extends Account<InteracETransferAccountPayload, FiatPaymentMethod> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.INTERAC_E_TRANSFER);

    public InteracETransferAccount(long creationDate, String accountName, InteracETransferAccountPayload accountPayload) {
        super(creationDate, accountName, PAYMENT_METHOD, accountPayload);
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean ignoreAnnotation) {
        return getAccountBuilder(ignoreAnnotation)
                .setInteracETransferAccount(bisq.account.protobuf.InteracETransferAccount.newBuilder());
    }

    @Override
    public bisq.account.protobuf.Account toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
    }

    public static InteracETransferAccount fromProto(bisq.account.protobuf.Account proto) {
        return new InteracETransferAccount(
                proto.getCreationDate(),
                proto.getAccountName(),
                InteracETransferAccountPayload.fromProto(proto.getAccountPayload())
        );
    }
}
