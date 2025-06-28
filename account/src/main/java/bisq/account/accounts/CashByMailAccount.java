package bisq.account.accounts;

import bisq.account.payment_method.FiatPaymentMethod;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class CashByMailAccount extends Account<FiatPaymentMethod, CashByMailAccountPayload> {
    public CashByMailAccount(long creationDate, String accountName, CashByMailAccountPayload accountPayload) {
        super(creationDate, accountName, accountPayload);
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean serializeForHash) {
        return getAccountBuilder(serializeForHash)
                .setCashByMailAccount(toCashByMailAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.CashByMailAccount toCashByMailAccountProto(boolean serializeForHash) {
        return resolveBuilder(getCashByMailAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.CashByMailAccount.Builder getCashByMailAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.CashByMailAccount.newBuilder();
    }

    public static CashByMailAccount fromProto(bisq.account.protobuf.Account proto) {
        return new CashByMailAccount(proto.getCreationDate(),
                proto.getAccountName(),
                CashByMailAccountPayload.fromProto(proto.getAccountPayload())
        );
    }
}
