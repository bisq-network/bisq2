package bisq.account.accounts;

import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.protobuf.AccountPayload;
import bisq.common.validation.PhoneNumberValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class HalCashAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {

    private final String mobileNr;

    public HalCashAccountPayload(String id, String countryCode,  String mobileNr) {
        super(id, countryCode);
        this.mobileNr = mobileNr;
    }

    @Override
    public void verify() {
        super.verify();

        checkArgument(PhoneNumberValidation.isValid(mobileNr, "ES"));
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setHalCashAccountPayload(
                toHalCashAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.HalCashAccountPayload toHalCashAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getHalCashAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.HalCashAccountPayload.Builder getHalCashAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.HalCashAccountPayload.newBuilder()
                .setMobileNr(mobileNr);
    }

    public static HalCashAccountPayload fromProto(AccountPayload proto) {
        bisq.account.protobuf.CountryBasedAccountPayload countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        bisq.account.protobuf.HalCashAccountPayload pixAccountPayload = countryBasedAccountPayload.getHalCashAccountPayload();
        return new HalCashAccountPayload(proto.getId(),
                countryBasedAccountPayload.getCountryCode(),
                pixAccountPayload.getMobileNr()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.HAL_CASH);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("user.paymentAccounts.mobileNr"), mobileNr
        ).toString();
    }
}
