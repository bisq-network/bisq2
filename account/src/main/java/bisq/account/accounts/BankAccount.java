package bisq.account.accounts;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.common.locale.Country;
import bisq.common.proto.UnresolvableProtobufMessageException;

public abstract class BankAccount<P extends BankAccountPayload> extends CountryBasedAccount<P, FiatPaymentMethod> {
    public BankAccount(String accountName, FiatPaymentMethod paymentMethod, P payload, Country country) {
        super(accountName, paymentMethod, payload, country);
    }

    protected bisq.account.protobuf.BankAccount.Builder getBankAccountBuilder() {
        return bisq.account.protobuf.BankAccount.newBuilder();
    }

    public static BankAccount<?> fromProto(bisq.account.protobuf.Account proto) {
        switch (proto.getCountryBasedAccount().getBankAccount().getMessageCase()) {
            case ACHTRANSFERACCOUNT:
                return AchTransferAccount.fromProto(proto);
            case NATIONALBANKACCOUNT:
                return NationalBankAccount.fromProto(proto);
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }
}
