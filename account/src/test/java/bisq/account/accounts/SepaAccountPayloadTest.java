package bisq.account.accounts;

import bisq.account.payment_method.FiatPaymentRailUtil;
import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.CountryBasedAccountPayload;
import bisq.account.protobuf.SepaAccountPayload;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SepaAccountPayloadTest {

    private static final String VALID_ID = "59a34c1e-4b5d-42c7-8fde-6931839e5682";
    private static final String VALID_PAYMENT_METHOD = "SEPA";
    private static final String VALID_HOLDER_NAME = "Max Mustermann";
    private static final String VALID_IBAN = "DE89370400440532013000";
    private static final String VALID_BIC = "COBADEFFXXX";
    private static final String VALID_COUNTRY_CODE = "DE";
    private static final List<String> VALID_ACCEPTED_COUNTRY_CODES = FiatPaymentRailUtil.getSepaEuroCountries();

    private static final AccountPayload PROTO = AccountPayload.newBuilder()
            .setId(VALID_ID)
            .setPaymentMethodName(VALID_PAYMENT_METHOD)
            .setCountryBasedAccountPayload(
                    CountryBasedAccountPayload.newBuilder()
                            .setCountryCode(VALID_COUNTRY_CODE)
                            .setSepaAccountPayload(SepaAccountPayload.newBuilder()
                                    .setHolderName(VALID_HOLDER_NAME)
                                    .setIban(VALID_IBAN)
                                    .setBic(VALID_BIC)
                                    .addAllAcceptedCountryCodes(VALID_ACCEPTED_COUNTRY_CODES)))
            .build();

    private static final bisq.account.accounts.SepaAccountPayload PAYLOAD =
            new bisq.account.accounts.SepaAccountPayload(
                    VALID_ID,
                    VALID_PAYMENT_METHOD,
                    VALID_HOLDER_NAME,
                    VALID_IBAN,
                    VALID_BIC,
                    VALID_COUNTRY_CODE,
                    VALID_ACCEPTED_COUNTRY_CODES
            );

    @Test
    void testToProto() {
        assertEquals(PROTO, PAYLOAD.completeProto());
    }

    @Test
    void testFromProto() {
        assertEquals(PAYLOAD, bisq.account.accounts.SepaAccountPayload.fromProto(PROTO));
    }

    @Test
    void testValidation_ValidPayload() {
        new bisq.account.accounts.SepaAccountPayload(
                VALID_ID,
                VALID_PAYMENT_METHOD,
                VALID_HOLDER_NAME,
                VALID_IBAN,
                VALID_BIC,
                VALID_COUNTRY_CODE,
                VALID_ACCEPTED_COUNTRY_CODES
        );
    }

    @Test
    void testValidation_InvalidIban() {
        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaAccountPayload(
                        VALID_ID,
                        VALID_PAYMENT_METHOD,
                        VALID_HOLDER_NAME,
                        "DE12345", // Invalid IBAN format
                        VALID_BIC,
                        VALID_COUNTRY_CODE,
                        VALID_ACCEPTED_COUNTRY_CODES
                )
        );

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaAccountPayload(
                        VALID_ID,
                        VALID_PAYMENT_METHOD,
                        VALID_HOLDER_NAME,
                        "DE89370400440532013001", // Changed last digit to make checksum fail
                        VALID_BIC,
                        VALID_COUNTRY_CODE,
                        VALID_ACCEPTED_COUNTRY_CODES
                )
        );

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaAccountPayload(
                        VALID_ID,
                        VALID_PAYMENT_METHOD,
                        VALID_HOLDER_NAME,
                        "US89370400440532013000", // US is not a SEPA country
                        VALID_BIC,
                        VALID_COUNTRY_CODE,
                        VALID_ACCEPTED_COUNTRY_CODES
                )
        );
    }

    @Test
    void testValidation_InvalidBic() {
        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaAccountPayload(
                        VALID_ID,
                        VALID_PAYMENT_METHOD,
                        VALID_HOLDER_NAME,
                        VALID_IBAN,
                        "COBAD", // Invalid BIC length
                        VALID_COUNTRY_CODE,
                        VALID_ACCEPTED_COUNTRY_CODES
                )
        );

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaAccountPayload(
                        VALID_ID,
                        VALID_PAYMENT_METHOD,
                        VALID_HOLDER_NAME,
                        VALID_IBAN,
                        "COB4DEFFXXX", // Contains a number in first 6 chars
                        VALID_COUNTRY_CODE,
                        VALID_ACCEPTED_COUNTRY_CODES
                )
        );

        // Invalid BIC - Revolut BIC codes not supported
        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaAccountPayload(
                        VALID_ID,
                        VALID_PAYMENT_METHOD,
                        VALID_HOLDER_NAME,
                        VALID_IBAN,
                        "REVOGB21XXX", // Revolut BIC
                        VALID_COUNTRY_CODE,
                        VALID_ACCEPTED_COUNTRY_CODES
                )
        );
    }

    @Test
    void testValidation_CountryCodes() {
        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaAccountPayload(
                        VALID_ID,
                        VALID_PAYMENT_METHOD,
                        VALID_HOLDER_NAME,
                        VALID_IBAN,
                        VALID_BIC,
                        VALID_COUNTRY_CODE,
                        Collections.emptyList() // No accepted countries
                )
        );

        List<String> invalidCountryCodes = List.of("DE", "FR", "INVALID");
        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaAccountPayload(
                        VALID_ID,
                        VALID_PAYMENT_METHOD,
                        VALID_HOLDER_NAME,
                        VALID_IBAN,
                        VALID_BIC,
                        VALID_COUNTRY_CODE,
                        invalidCountryCodes
                )
        );

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaAccountPayload(
                        VALID_ID,
                        VALID_PAYMENT_METHOD,
                        VALID_HOLDER_NAME,
                        VALID_IBAN, // DE IBAN
                        VALID_BIC,
                        "FR", // But FR country code
                        VALID_ACCEPTED_COUNTRY_CODES
                )
        );
    }

    @Test
    void testGetters() {
        assertEquals(VALID_ID, PAYLOAD.getId());
        assertEquals(VALID_PAYMENT_METHOD, PAYLOAD.getPaymentMethodName());
        assertEquals(VALID_HOLDER_NAME, PAYLOAD.getHolderName());
        assertEquals(VALID_IBAN, PAYLOAD.getIban());
        assertEquals(VALID_BIC, PAYLOAD.getBic());
        assertEquals(VALID_COUNTRY_CODE, PAYLOAD.getCountryCode());
        assertEquals(VALID_ACCEPTED_COUNTRY_CODES, PAYLOAD.getAcceptedCountryCodes());
    }
}