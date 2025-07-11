package bisq.account.accounts;

import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.protobuf.AccountPayload;
import bisq.common.validation.NetworkDataValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class SwishAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {
    public static final int HOLDER_NAME_MIN_LENGTH = 2;
    public static final int HOLDER_NAME_MAX_LENGTH = 70;
    public static final int swish_KEY_MIN_LENGTH = 2;
    public static final int swish_KEY_MAX_LENGTH = 100;

    private final String holderName;
    private final String mobileNr;

    public SwishAccountPayload(String id, String countryCode, String holderName, String mobileNr) {
        super(id, countryCode);
        this.holderName = holderName;
        this.mobileNr = mobileNr;
    }

    @Override
    public void verify() {
        super.verify();

        NetworkDataValidation.validateRequiredText(holderName, HOLDER_NAME_MIN_LENGTH, HOLDER_NAME_MAX_LENGTH);
        NetworkDataValidation.validateRequiredText(mobileNr, swish_KEY_MIN_LENGTH, swish_KEY_MAX_LENGTH);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setSwishAccountPayload(
                toSwishAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.SwishAccountPayload toSwishAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getSwishAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.SwishAccountPayload.Builder getSwishAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.SwishAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setMobileNr(mobileNr);
    }

    public static SwishAccountPayload fromProto(AccountPayload proto) {
        bisq.account.protobuf.CountryBasedAccountPayload countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        bisq.account.protobuf.SwishAccountPayload swishAccountPayload = countryBasedAccountPayload.getSwishAccountPayload();
        return new SwishAccountPayload(proto.getId(),
                countryBasedAccountPayload.getCountryCode(),
                swishAccountPayload.getHolderName(),
                swishAccountPayload.getMobileNr()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SWISH);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("user.paymentAccounts.holderName"), holderName,
                Res.get("user.paymentAccounts.mobileNr"), mobileNr
        ).toString();
    }
}
