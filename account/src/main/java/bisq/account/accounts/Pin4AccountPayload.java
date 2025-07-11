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
public final class Pin4AccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {

    private final String mobileNr;

    public Pin4AccountPayload(String id, String countryCode, String mobileNr) {
        super(id, countryCode);
        this.mobileNr = mobileNr;
    }

    @Override
    public void verify() {
        super.verify();

        checkArgument(PhoneNumberValidation.isValid(mobileNr, "PL"));
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setPin4AccountPayload(
                toPin4AccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.Pin4AccountPayload toPin4AccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getPin4AccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.Pin4AccountPayload.Builder getPin4AccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.Pin4AccountPayload.newBuilder()
                .setMobileNr(mobileNr);
    }

    public static Pin4AccountPayload fromProto(AccountPayload proto) {
        bisq.account.protobuf.CountryBasedAccountPayload countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        bisq.account.protobuf.Pin4AccountPayload pixAccountPayload = countryBasedAccountPayload.getPin4AccountPayload();
        return new Pin4AccountPayload(proto.getId(),
                countryBasedAccountPayload.getCountryCode(),
                pixAccountPayload.getMobileNr()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.PIN_4);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("user.paymentAccounts.mobileNr"), mobileNr
        ).toString();
    }
}
