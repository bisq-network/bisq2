package bisq.account.accounts;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class CashDepositAccount extends BankAccount<CashDepositAccountPayload> {
    public CashDepositAccount(String id, long creationDate, String accountName, CashDepositAccountPayload accountPayload) {
        super(id, creationDate, accountName, accountPayload);
    }

    @Override
    protected bisq.account.protobuf.BankAccount.Builder getBankAccountBuilder(boolean serializeForHash) {
        return super.getBankAccountBuilder(serializeForHash).setCashDepositAccount(
                toCashDepositAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.CashDepositAccount toCashDepositAccountProto(boolean serializeForHash) {
        return resolveBuilder(getCashDepositAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.CashDepositAccount.Builder getCashDepositAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.CashDepositAccount.newBuilder();
    }

    public static CashDepositAccount fromProto(bisq.account.protobuf.Account proto) {
        return new CashDepositAccount(proto.getId(),
                proto.getCreationDate(),
                proto.getAccountName(),
                CashDepositAccountPayload.fromProto(proto.getAccountPayload()));
    }
}
