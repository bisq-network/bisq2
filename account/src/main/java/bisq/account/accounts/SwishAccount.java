package bisq.account.accounts;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class SwishAccount extends CountryBasedAccount<SwishAccountPayload> {
    public SwishAccount(String id, long creationDate, String accountName, SwishAccountPayload accountPayload) {
        super(id, creationDate, accountName, accountPayload);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setSwishAccount(
                toSwishAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.SwishAccount toSwishAccountProto(boolean serializeForHash) {
        return resolveBuilder(getSwishAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.SwishAccount.Builder getSwishAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.SwishAccount.newBuilder();
    }

    public static SwishAccount fromProto(bisq.account.protobuf.Account proto) {
        return new SwishAccount(proto.getId(),
                proto.getCreationDate(),
                proto.getAccountName(),
                SwishAccountPayload.fromProto(proto.getAccountPayload()));
    }
}
