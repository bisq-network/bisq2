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
public final class UpiAccount extends CountryBasedAccount<UpiAccountPayload> {
    public UpiAccount(String id, long creationDate, String accountName, UpiAccountPayload accountPayload) {
        super(id, creationDate, accountName, accountPayload);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setUpiAccount(
                toUpiAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.UpiAccount toUpiAccountProto(boolean serializeForHash) {
        return resolveBuilder(getUpiAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.UpiAccount.Builder getUpiAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.UpiAccount.newBuilder();
    }

    public static UpiAccount fromProto(Account proto) {
        return new UpiAccount(proto.getId(),
                proto.getCreationDate(),
                proto.getAccountName(),
                UpiAccountPayload.fromProto(proto.getAccountPayload()));
    }
}
