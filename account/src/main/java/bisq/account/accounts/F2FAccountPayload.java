package bisq.account.accounts;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.common.validation.PaymentAccountValidation;
import lombok.Getter;

@Getter
public class F2FAccountPayload extends CountryBasedAccountPayload implements SelectableCurrencyAccountPayload {
    public static final int CITY_MIN_LENGTH = 2;
    public static final int CITY_MAX_LENGTH = 50;
    public static final int CONTACT_MIN_LENGTH = 5;
    public static final int CONTACT_MAX_LENGTH = 100;
    public static final int EXTRA_INFO_MIN_LENGTH = 1;
    public static final int EXTRA_INFO_MAX_LENGTH = 300;

    private final String selectedCurrencyCode;
    private final String city;
    private final String contact;
    private final String extraInfo;

    public F2FAccountPayload(String id,
                             String countryCode,
                             String selectedCurrencyCode,
                             String city,
                             String contact,
                             String extraInfo) {
        super(id, countryCode);
        this.selectedCurrencyCode = selectedCurrencyCode;
        this.city = city;
        this.contact = contact;
        this.extraInfo = extraInfo;
        verify();
    }

    @Override
    public void verify() {
        super.verify();
        PaymentAccountValidation.validateCurrencyCode(selectedCurrencyCode);
        NetworkDataValidation.validateRequiredText(city, CITY_MIN_LENGTH, CITY_MAX_LENGTH);
        NetworkDataValidation.validateRequiredText(contact, CONTACT_MIN_LENGTH, CONTACT_MAX_LENGTH);
        NetworkDataValidation.validateText(extraInfo, EXTRA_INFO_MIN_LENGTH, EXTRA_INFO_MAX_LENGTH);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setF2FAccountPayload(
                toF2FAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.F2FAccountPayload toF2FAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getF2FAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.F2FAccountPayload.Builder getF2FAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.F2FAccountPayload.newBuilder()
                .setSelectedCurrencyCode(selectedCurrencyCode)
                .setCity(city)
                .setContact(contact)
                .setExtraInfo(extraInfo);
    }

    public static F2FAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        bisq.account.protobuf.CountryBasedAccountPayload countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        bisq.account.protobuf.F2FAccountPayload f2fAccountPayload = countryBasedAccountPayload.getF2FAccountPayload();
        return new F2FAccountPayload(
                proto.getId(),
                countryBasedAccountPayload.getCountryCode(),
                f2fAccountPayload.getSelectedCurrencyCode(),
                f2fAccountPayload.getCity(),
                f2fAccountPayload.getContact(),
                f2fAccountPayload.getExtraInfo()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.F2F);
    }

    @Override
    public String getDefaultAccountName() {
        return paymentRailName + "-" + countryCode + "/" + StringUtils.truncate(city, 5);
    }
}
