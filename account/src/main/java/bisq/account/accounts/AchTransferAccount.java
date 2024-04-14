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
    protected bisq.account.protobuf.BankAccount.Builder getBankAccountBuilder(boolean serializeForHash) {
        return super.getBankAccountBuilder(serializeForHash).setAchTransferAccount(
                toAchTransferAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.AchTransferAccount toAchTransferAccountProto(boolean serializeForHash) {
        return resolveBuilder(getAchTransferAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.AchTransferAccount.Builder getAchTransferAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.AchTransferAccount.newBuilder();
    }

    public static AchTransferAccount fromProto(bisq.account.protobuf.Account proto) {
        return new AchTransferAccount(
                proto.getAccountName(),
                AchTransferAccountPayload.fromProto(proto.getAccountPayload()),
                Country.fromProto(proto.getCountryBasedAccount().getCountry()));
    }
}
