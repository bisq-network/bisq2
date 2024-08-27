package bisq.account.accounts;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.common.locale.Country;
import bisq.common.proto.UnresolvableProtobufMessageException;

public abstract class BankAccount<P extends BankAccountPayload> extends CountryBasedAccount<P, FiatPaymentMethod> {
    public BankAccount(String accountName, FiatPaymentMethod paymentMethod, P payload, Country country) {
        super(accountName, paymentMethod, payload, country);
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

    public static BankAccount<?> fromProto(bisq.account.protobuf.Account proto) {
        return switch (proto.getCountryBasedAccount().getBankAccount().getMessageCase()) {
            case ACHTRANSFERACCOUNT -> AchTransferAccount.fromProto(proto);
            case NATIONALBANKACCOUNT -> NationalBankAccount.fromProto(proto);
            case CASHDEPOSITACCOUNT -> CashDepositAccount.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }
}
