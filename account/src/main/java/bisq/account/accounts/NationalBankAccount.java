package bisq.account.accounts;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public class NationalBankAccount extends BankAccount<NationalBankAccountPayload> {
    public NationalBankAccount(long creationDate, String accountName, NationalBankAccountPayload accountPayload) {
        super(creationDate, accountName, accountPayload);
    }

    @Override
    protected bisq.account.protobuf.BankAccount.Builder getBankAccountBuilder(boolean serializeForHash) {
        return super.getBankAccountBuilder(serializeForHash).setNationalBankAccount(
                toNationalBankAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.NationalBankAccount toNationalBankAccountProto(boolean serializeForHash) {
        return resolveBuilder(getNationalBankAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.NationalBankAccount.Builder getNationalBankAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.NationalBankAccount.newBuilder();
    }

    public static NationalBankAccount fromProto(bisq.account.protobuf.Account proto) {
        return new NationalBankAccount(proto.getCreationDate(),
                proto.getAccountName(),
                NationalBankAccountPayload.fromProto(proto.getAccountPayload()));
    }
}
