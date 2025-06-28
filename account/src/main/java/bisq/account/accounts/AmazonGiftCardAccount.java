package bisq.account.accounts;

import bisq.account.protobuf.Account;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class AmazonGiftCardAccount extends CountryBasedAccount<AmazonGiftCardAccountPayload> {
    public AmazonGiftCardAccount(long creationDate, String accountName, AmazonGiftCardAccountPayload accountPayload) {
        super(creationDate, accountName, accountPayload);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setAmazonGiftCardAccount(
                toAmazonGiftCardAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.AmazonGiftCardAccount toAmazonGiftCardAccountProto(boolean serializeForHash) {
        return resolveBuilder(getAmazonGiftCardAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.AmazonGiftCardAccount.Builder getAmazonGiftCardAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.AmazonGiftCardAccount.newBuilder();
    }

    @Override
    public Account.Builder getBuilder(boolean serializeForHash) {
        return getAccountBuilder(serializeForHash)
                .setCountryBasedAccount(getCountryBasedAccountBuilder(serializeForHash)
                        .setAmazonGiftCardAccount(bisq.account.protobuf.AmazonGiftCardAccount.newBuilder()));
    }

    public static AmazonGiftCardAccount fromProto(bisq.account.protobuf.Account proto) {
        return new AmazonGiftCardAccount(proto.getCreationDate(),
                proto.getAccountName(),
                AmazonGiftCardAccountPayload.fromProto(proto.getAccountPayload()));
    }
}
