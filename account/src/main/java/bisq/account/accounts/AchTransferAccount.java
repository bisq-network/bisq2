package bisq.account.accounts;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.common.locale.Country;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public final class AchTransferAccount extends BankAccount<AchTransferAccountPayload> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER);

    public AchTransferAccount(String accountName, AchTransferAccountPayload payload, Country country) {
        super(accountName, PAYMENT_METHOD, payload, country);
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean ignoreAnnotation) {
        return getAccountBuilder(ignoreAnnotation)
                .setCountryBasedAccount(getCountryBasedAccountBuilder(ignoreAnnotation)
                        .setBankAccount(getBankAccountBuilder(ignoreAnnotation).setAchTransferAccount(
                                bisq.account.protobuf.AchTransferAccount.newBuilder())));
    }

    @Override
    public bisq.account.protobuf.Account toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
    }

    public static AchTransferAccount fromProto(bisq.account.protobuf.Account proto) {
        return new AchTransferAccount(
                proto.getAccountName(),
                AchTransferAccountPayload.fromProto(proto.getAccountPayload()),
                Country.fromProto(proto.getCountryBasedAccount().getCountry()));
    }
}
