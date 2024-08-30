package bisq.account.accounts;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.common.locale.Country;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public class NationalBankAccount extends BankAccount<NationalBankAccountPayload> {
    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);

    public NationalBankAccount(String accountName, NationalBankAccountPayload payload, Country country) {
        super(accountName, PAYMENT_METHOD, payload, country);
    }

    @Override
    protected bisq.account.protobuf.BankAccount.Builder getBankAccountBuilder(boolean serializeForHash) {
        return super.getBankAccountBuilder(serializeForHash).setNationalBankAccount(
                toNationalBankAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.NationalBankAccount toNationalBankAccountProto(boolean serializeForHash) {
        return resolveBuilder(getNationalBankAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.NationalBankAccount.Builder getNationalBankAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.NationalBankAccount.newBuilder();
    }

    public static NationalBankAccount fromProto(bisq.account.protobuf.Account proto) {
        return new NationalBankAccount(
                proto.getAccountName(),
                NationalBankAccountPayload.fromProto(proto.getAccountPayload()),
                Country.fromProto(proto.getCountryBasedAccount().getCountry()));
    }
}
