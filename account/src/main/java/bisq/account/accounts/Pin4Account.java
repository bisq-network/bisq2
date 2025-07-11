package bisq.account.accounts;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class Pin4Account extends CountryBasedAccount<Pin4AccountPayload> {
    public Pin4Account(String id, long creationDate, String accountName, Pin4AccountPayload accountPayload) {
        super(id, creationDate, accountName, accountPayload);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setPin4Account(
                toPin4AccountProto(serializeForHash));
    }

    private bisq.account.protobuf.Pin4Account toPin4AccountProto(boolean serializeForHash) {
        return resolveBuilder(getPin4AccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.Pin4Account.Builder getPin4AccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.Pin4Account.newBuilder();
    }

    public static Pin4Account fromProto(bisq.account.protobuf.Account proto) {
        return new Pin4Account(proto.getId(),
                proto.getCreationDate(),
                proto.getAccountName(),
                Pin4AccountPayload.fromProto(proto.getAccountPayload()));
    }
}
