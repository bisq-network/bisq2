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
public final class StrikeAccount extends CountryBasedAccount<StrikeAccountPayload> {
    public StrikeAccount(String id, long creationDate, String accountName, StrikeAccountPayload accountPayload) {
        super(id, creationDate, accountName, accountPayload);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setStrikeAccount(
                toStrikeAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.StrikeAccount toStrikeAccountProto(boolean serializeForHash) {
        return resolveBuilder(getStrikeAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.StrikeAccount.Builder getStrikeAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.StrikeAccount.newBuilder();
    }

    public static StrikeAccount fromProto(Account proto) {
        return new StrikeAccount(proto.getId(),
                proto.getCreationDate(),
                proto.getAccountName(),
                StrikeAccountPayload.fromProto(proto.getAccountPayload()));
    }
}
