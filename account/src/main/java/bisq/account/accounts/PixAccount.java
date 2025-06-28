package bisq.account.accounts;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class PixAccount extends CountryBasedAccount<PixAccountPayload> {
    public PixAccount(long creationDate, String accountName, PixAccountPayload accountPayload) {
        super(creationDate, accountName, accountPayload);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setPixAccount(
                toPixAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.PixAccount toPixAccountProto(boolean serializeForHash) {
        return resolveBuilder(getPixAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.PixAccount.Builder getPixAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.PixAccount.newBuilder();
    }

    public static PixAccount fromProto(bisq.account.protobuf.Account proto) {
        return new PixAccount(proto.getCreationDate(),
                proto.getAccountName(),
                PixAccountPayload.fromProto(proto.getAccountPayload()));
    }
}
