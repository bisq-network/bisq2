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
public final class BizumAccount extends CountryBasedAccount<BizumAccountPayload> {
    public BizumAccount(long creationDate, String accountName, BizumAccountPayload accountPayload) {
        super(creationDate, accountName, accountPayload);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setBizumAccount(
                toBizumAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.BizumAccount toBizumAccountProto(boolean serializeForHash) {
        return resolveBuilder(getBizumAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.BizumAccount.Builder getBizumAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.BizumAccount.newBuilder();
    }

    public static BizumAccount fromProto(Account proto) {
        return new BizumAccount(proto.getCreationDate(),
                proto.getAccountName(),
                BizumAccountPayload.fromProto(proto.getAccountPayload()));
    }
}
