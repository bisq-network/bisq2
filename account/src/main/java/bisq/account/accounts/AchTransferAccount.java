package bisq.account.accounts;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public final class AchTransferAccount extends BankAccount<AchTransferAccountPayload> {
    public AchTransferAccount(long creationDate, String accountName, AchTransferAccountPayload accountPayload) {
        super(creationDate, accountName, accountPayload);
    }

    @Override
    protected bisq.account.protobuf.BankAccount.Builder getBankAccountBuilder(boolean serializeForHash) {
        return super.getBankAccountBuilder(serializeForHash).setAchTransferAccount(
                toAchTransferAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.AchTransferAccount toAchTransferAccountProto(boolean serializeForHash) {
        return resolveBuilder(getAchTransferAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.AchTransferAccount.Builder getAchTransferAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.AchTransferAccount.newBuilder();
    }

    public static AchTransferAccount fromProto(bisq.account.protobuf.Account proto) {
        return new AchTransferAccount(proto.getCreationDate(),
                proto.getAccountName(),
                AchTransferAccountPayload.fromProto(proto.getAccountPayload()));
    }
}
