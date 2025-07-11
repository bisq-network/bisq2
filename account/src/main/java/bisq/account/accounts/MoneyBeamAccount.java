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
public final class MoneyBeamAccount extends CountryBasedAccount<MoneyBeamAccountPayload> {
    public MoneyBeamAccount(String id, long creationDate, String accountName, MoneyBeamAccountPayload accountPayload) {
        super(id, creationDate, accountName, accountPayload);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setMoneyBeamAccount(
                toMoneyBeamAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.MoneyBeamAccount toMoneyBeamAccountProto(boolean serializeForHash) {
        return resolveBuilder(getMoneyBeamAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.MoneyBeamAccount.Builder getMoneyBeamAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.MoneyBeamAccount.newBuilder();
    }

    public static MoneyBeamAccount fromProto(Account proto) {
        return new MoneyBeamAccount(proto.getId(),
                proto.getCreationDate(),
                proto.getAccountName(),
                MoneyBeamAccountPayload.fromProto(proto.getAccountPayload()));
    }
}
