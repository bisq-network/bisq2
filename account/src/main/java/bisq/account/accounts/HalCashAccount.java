package bisq.account.accounts;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class HalCashAccount extends CountryBasedAccount<HalCashAccountPayload> {
    public HalCashAccount(String id, long creationDate, String accountName, HalCashAccountPayload accountPayload) {
        super(id, creationDate, accountName, accountPayload);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setHalCashAccount(
                toHalCashAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.HalCashAccount toHalCashAccountProto(boolean serializeForHash) {
        return resolveBuilder(getHalCashAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.HalCashAccount.Builder getHalCashAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.HalCashAccount.newBuilder();
    }

    public static HalCashAccount fromProto(bisq.account.protobuf.Account proto) {
        return new HalCashAccount(proto.getId(),
                proto.getCreationDate(),
                proto.getAccountName(),
                HalCashAccountPayload.fromProto(proto.getAccountPayload()));
    }
}
