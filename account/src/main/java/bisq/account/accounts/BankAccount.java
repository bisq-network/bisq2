package bisq.account.accounts;

import bisq.account.protobuf.Account;
import bisq.common.currency.TradeCurrency;
import bisq.common.proto.UnresolvableProtobufMessageException;

public abstract class BankAccount<P extends BankAccountPayload> extends CountryBasedAccount<P> {
    public BankAccount(String accountName, P payload) {
        super(accountName, payload);
    }

    public BankAccount(String accountName, P payload, TradeCurrency tradeCurrency) {
        super(accountName, payload);
    }

    protected bisq.account.protobuf.BankAccount.Builder getBankAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.BankAccount.newBuilder();
    }

    protected bisq.account.protobuf.BankAccount toBankAccountProto(boolean serializeForHash) {
        return resolveBuilder(getBankAccountBuilder(serializeForHash), serializeForHash).build();
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash)
                .setBankAccount(toBankAccountProto(serializeForHash));
    }

    public static BankAccount<?> fromProto(Account proto) {
        return switch (proto.getCountryBasedAccount().getBankAccount().getMessageCase()) {
            case ACHTRANSFERACCOUNT -> AchTransferAccount.fromProto(proto);
            case NATIONALBANKACCOUNT -> NationalBankAccount.fromProto(proto);
            case CASHDEPOSITACCOUNT -> CashDepositAccount.fromProto(proto);
            case SAMEBANKACCOUNT -> SameBankAccount.fromProto(proto);
            case DOMESTICWIRETRANSFERACCOUNT -> DomesticWireTransferAccount.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }
}
