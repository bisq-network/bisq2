package bisq.account.accounts;

public class F2FAccountPayload extends CountryBasedAccountPayload {

    private final String city;
    private final String contact;
    private final String extraInfo;

    public F2FAccountPayload(String id, String paymentMethodName, String countryCode, String city, String contact, String extraInfo) {
        super(id, paymentMethodName, countryCode);
        this.city = city;
        this.contact = contact;
        this.extraInfo = extraInfo;
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
}
