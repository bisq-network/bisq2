package bisq.account.accounts;

import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.protobuf.AccountPayload;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class MoneyBeamAccountPayload extends CountryBasedAccountPayload implements SelectableCurrencyAccountPayload {
    private final String selectedCurrencyCode;
    private final String accountId;

    public MoneyBeamAccountPayload(String id, String countryCode, String selectedCurrencyCode, String accountId) {
        super(id, countryCode);
        this.selectedCurrencyCode = selectedCurrencyCode;
        this.accountId = accountId;
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setMoneyBeamAccountPayload(
                toMoneyBeamAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.MoneyBeamAccountPayload toMoneyBeamAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getMoneyBeamAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.MoneyBeamAccountPayload.Builder getMoneyBeamAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.MoneyBeamAccountPayload.newBuilder()
                .setSelectedCurrencyCode(selectedCurrencyCode)
                .setAccountId(accountId);
    }

    public static MoneyBeamAccountPayload fromProto(AccountPayload proto) {
        var moneyBeamProto = proto.getCountryBasedAccountPayload().getMoneyBeamAccountPayload();
        return new MoneyBeamAccountPayload(
                proto.getId(),
                proto.getCountryBasedAccountPayload().getCountryCode(),
                moneyBeamProto.getSelectedCurrencyCode(),
                moneyBeamProto.getAccountId());
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.MONEY_BEAM);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("user.paymentAccounts.moneyBeam.accountId"), accountId
        ).toString();
    }
}
