package bisq.account.accounts.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.timestamp.KeyType;
import bisq.security.keys.KeyPairProtoUtil;
import lombok.EqualsAndHashCode;

import java.security.KeyPair;

@EqualsAndHashCode(callSuper = true)
public final class AchTransferAccount extends BankAccount<AchTransferAccountPayload> {
    public AchTransferAccount(String id,
                              long creationDate,
                              String accountName,
                              AchTransferAccountPayload accountPayload,
                              KeyPair keyPair,
                              KeyType keyType,
                              AccountOrigin accountOrigin) {
        super(id, creationDate, accountName, accountPayload, keyPair, keyType, accountOrigin);
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
        KeyType keyType = KeyType.fromProto(proto.getKeyType());
        AccountOrigin accountOrigin = AccountOrigin.fromProto(proto.getAccountOrigin());
        return new AchTransferAccount(proto.getId(),
                proto.getCreationDate(),
                proto.getAccountName(),
                AchTransferAccountPayload.fromProto(proto.getAccountPayload()),
                KeyPairProtoUtil.fromProto(proto.getKeyPair(), keyType.getKeyAlgorithm()),
                keyType,
                accountOrigin);
    }
}
