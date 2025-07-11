package bisq.account.accounts;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class PromptPayAccount extends CountryBasedAccount<PromptPayAccountPayload> {
    public PromptPayAccount(String id, long creationDate, String accountName, PromptPayAccountPayload accountPayload) {
        super(id, creationDate, accountName, accountPayload);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setPromptPayAccount(
                toPromptPayAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.PromptPayAccount toPromptPayAccountProto(boolean serializeForHash) {
        return resolveBuilder(getPromptPayAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.PromptPayAccount.Builder getPromptPayAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.PromptPayAccount.newBuilder();
    }

    public static PromptPayAccount fromProto(bisq.account.protobuf.Account proto) {
        return new PromptPayAccount(proto.getId(),
                proto.getCreationDate(),
                proto.getAccountName(),
                PromptPayAccountPayload.fromProto(proto.getAccountPayload()));
    }
}
