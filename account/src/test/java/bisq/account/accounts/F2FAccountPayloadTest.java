package bisq.account.accounts;

import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.CountryBasedAccountPayload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class F2FAccountPayloadTest {

    private static final String VALID_ID = "id";
    private static final String VALID_PAYMENT_METHOD = "paymentMethodName";
    private static final String VALID_COUNTRY_CODE = "Code";
    private static final String VALID_CITY = "city";
    private static final String VALID_CONTACT = "contact";
    private static final String VALID_EXTRA_INFO = "extraInfo";

    @Test
    void testProtoSerialization() {
        F2FAccountPayload payload = new F2FAccountPayload(
                VALID_ID,
                VALID_PAYMENT_METHOD,
                VALID_COUNTRY_CODE,
                VALID_CITY,
                VALID_CONTACT,
                VALID_EXTRA_INFO);

        AccountPayload proto = payload.toProto(false);

        assertEquals(VALID_ID, proto.getId());
        assertEquals(VALID_PAYMENT_METHOD, proto.getPaymentMethodName());

        CountryBasedAccountPayload countryBasedProto = proto.getCountryBasedAccountPayload();
        assertEquals(VALID_COUNTRY_CODE, countryBasedProto.getCountryCode());

        bisq.account.protobuf.F2FAccountPayload f2fProto = countryBasedProto.getF2FAccountPayload();
        assertEquals(VALID_CITY, f2fProto.getCity());
        assertEquals(VALID_CONTACT, f2fProto.getContact());
        assertEquals(VALID_EXTRA_INFO, f2fProto.getExtraInfo());

        F2FAccountPayload fromProto = F2FAccountPayload.fromProto(proto);

        AccountPayload roundTripProto = fromProto.toProto(false);

        assertEquals(proto.getId(), roundTripProto.getId());
        assertEquals(proto.getPaymentMethodName(), roundTripProto.getPaymentMethodName());

        CountryBasedAccountPayload roundTripCountryProto = roundTripProto.getCountryBasedAccountPayload();
        assertEquals(countryBasedProto.getCountryCode(), roundTripCountryProto.getCountryCode());

        bisq.account.protobuf.F2FAccountPayload roundTripF2fProto = roundTripCountryProto.getF2FAccountPayload();
        assertEquals(f2fProto.getCity(), roundTripF2fProto.getCity());
        assertEquals(f2fProto.getContact(), roundTripF2fProto.getContact());
        assertEquals(f2fProto.getExtraInfo(), roundTripF2fProto.getExtraInfo());
    }

    @Test
    void testValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                new F2FAccountPayload(VALID_ID, VALID_PAYMENT_METHOD, VALID_COUNTRY_CODE,
                        "", VALID_CONTACT, VALID_EXTRA_INFO));

        assertThrows(IllegalArgumentException.class, () ->
                new F2FAccountPayload(VALID_ID, VALID_PAYMENT_METHOD, VALID_COUNTRY_CODE,
                        VALID_CITY, "", VALID_EXTRA_INFO));

        String longText = "a".repeat(101);
        assertThrows(IllegalArgumentException.class, () ->
                new F2FAccountPayload(VALID_ID, VALID_PAYMENT_METHOD, VALID_COUNTRY_CODE,
                        longText, VALID_CONTACT, VALID_EXTRA_INFO));

        assertThrows(IllegalArgumentException.class, () ->
                new F2FAccountPayload(VALID_ID, VALID_PAYMENT_METHOD, VALID_COUNTRY_CODE,
                        VALID_CITY, longText, VALID_EXTRA_INFO));

        String veryLongText = "a".repeat(501);
        assertThrows(IllegalArgumentException.class, () ->
                new F2FAccountPayload(VALID_ID, VALID_PAYMENT_METHOD, VALID_COUNTRY_CODE,
                        VALID_CITY, VALID_CONTACT, veryLongText));
    }
}