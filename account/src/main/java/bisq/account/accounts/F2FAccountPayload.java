package bisq.account.accounts;

import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkDataValidation;
import lombok.Getter;

@Getter
public class F2FAccountPayload extends CountryBasedAccountPayload {
    public static final int CITY_MIN_LENGTH = 2;
    public static final int CITY_MAX_LENGTH = 50;
    public static final int CONTACT_MIN_LENGTH = 5;
    public static final int CONTACT_MAX_LENGTH = 100;
    public static final int EXTRA_INFO_MIN_LENGTH = 1;
    public static final int EXTRA_INFO_MAX_LENGTH = 300;

    private final String city;
    private final String contact;
    private final String extraInfo;

    public F2FAccountPayload(String id,
                             String paymentMethodName,
                             String countryCode,
                             String city,
                             String contact,
                             String extraInfo) {
        super(id, paymentMethodName, countryCode);
        this.city = city;
        this.contact = contact;
        this.extraInfo = extraInfo;
        verify();
    }

    @Override
    public void verify() {
        super.verify();
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
                .setCity(city)
                .setContact(contact)
                .setExtraInfo(extraInfo);
    }

    public static F2FAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        bisq.account.protobuf.CountryBasedAccountPayload countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        bisq.account.protobuf.F2FAccountPayload f2fAccountPayload = countryBasedAccountPayload.getF2FAccountPayload();
        return new F2FAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                countryBasedAccountPayload.getCountryCode(),
                f2fAccountPayload.getCity(),
                f2fAccountPayload.getContact(),
                f2fAccountPayload.getExtraInfo()
        );
    }

    @Override
    protected String getDefaultAccountName() {
        return paymentMethodName + "-" + countryCode + "/" + StringUtils.truncate(city, 5);
    }
}
