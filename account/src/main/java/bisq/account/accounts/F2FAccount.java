package bisq.account.accounts;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class F2FAccount extends CountryBasedAccount<F2FAccountPayload> {

    public F2FAccount(String accountName, F2FAccountPayload accountPayload) {
        super(accountName, accountPayload);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setF2FAccount(
                toF2FAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.F2FAccount toF2FAccountProto(boolean serializeForHash) {
        return resolveBuilder(getF2FAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.F2FAccount.Builder getF2FAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.F2FAccount.newBuilder();
    }

    public static F2FAccount fromProto(bisq.account.protobuf.Account proto) {
        return new F2FAccount(proto.getAccountName(),
                F2FAccountPayload.fromProto(proto.getAccountPayload()));
    }
}
