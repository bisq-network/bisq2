package bisq.account.accounts;

import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.CountryBasedAccountPayload;
import bisq.account.protobuf.SepaInstantAccountPayload;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SepaInstantAccountPayloadTest {

    private static final AccountPayload PROTO = AccountPayload.newBuilder()
            .setId("id")
            .setPaymentMethodName("SEPA_INSTANT")
            .setCountryBasedAccountPayload(
                    CountryBasedAccountPayload.newBuilder()
                            .setCountryCode("DE")
                            .setSepaInstantAccountPayload(SepaInstantAccountPayload.newBuilder()
                                    .setHolderName("holderName")
                                    .setIban("DE89370400440532013000")
                                    .setBic("DEUTDEBBXXX")
                                    .addAllAcceptedCountryCodes(List.of("DE", "FR", "IT"))))
            .build();

    private static final bisq.account.accounts.SepaInstantAccountPayload PAYLOAD =
            new bisq.account.accounts.SepaInstantAccountPayload(
                    "id",
                    "SEPA_INSTANT",
                    "holderName",
                    "DE89370400440532013000",
                    "DEUTDEBBXXX",
                    "DE",
                    List.of("DE", "FR", "IT"));

    @Test
    void testToProto() {
        assertEquals(PROTO, PAYLOAD.completeProto());
    }

    @Test
    void testFromProto() {
        assertEquals(PAYLOAD, bisq.account.accounts.SepaInstantAccountPayload.fromProto(PROTO));
    }

    @Test
    void testIbanValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaInstantAccountPayload(
                        "id",
                        "SEPA_INSTANT",
                        "holderName",
                        "INVALID-IBAN",
                        "DEUTDEBBXXX",
                        "DE",
                        List.of("DE")));

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaInstantAccountPayload(
                        "id",
                        "SEPA_INSTANT",
                        "holderName",
                        "DE12", // Too short
                        "DEUTDEBBXXX",
                        "DE",
                        List.of("DE")));

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaInstantAccountPayload(
                        "id",
                        "SEPA_INSTANT",
                        "holderName",
                        "DEAB370400440532013000", // Letters in check digits
                        "DEUTDEBBXXX",
                        "DE",
                        List.of("DE")));
    }

    @Test
    void testBicValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaInstantAccountPayload(
                        "id",
                        "SEPA_INSTANT",
                        "holderName",
                        "DE89370400440532013000",
                        "INVALID", // Too short
                        "DE",
                        List.of("DE")));

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaInstantAccountPayload(
                        "id",
                        "SEPA_INSTANT",
                        "holderName",
                        "DE89370400440532013000",
                        "12UTDEBBXXX", // Digits in bank code
                        "DE",
                        List.of("DE")));

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaInstantAccountPayload(
                        "id",
                        "SEPA_INSTANT",
                        "holderName",
                        "DE89370400440532013000",
                        "DEUT12BBXXX", // Digits in country code
                        "DE",
                        List.of("DE")));
    }

    @Test
    void testHolderNameValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaInstantAccountPayload(
                        "id",
                        "SEPA_INSTANT",
                        "X".repeat(101), // >100 chars
                        "DE89370400440532013000",
                        "DEUTDEBBXXX",
                        "DE",
                        List.of("DE")));
    }

    @Test
    void testCountryCodeValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaInstantAccountPayload(
                        "id",
                        "SEPA_INSTANT",
                        "holderName",
                        "DE89370400440532013000",
                        "DEUTDEBBXXX",
                        "DE",
                        List.of("INVALIDCOUNTRYCODE"))); // Too long country code
    }

    @Test
    void testEquals() {
        bisq.account.accounts.SepaInstantAccountPayload payload1 = new bisq.account.accounts.SepaInstantAccountPayload(
                "id",
                "SEPA_INSTANT",
                "holderName",
                "DE89370400440532013000",
                "DEUTDEBBXXX",
                "DE",
                List.of("DE", "FR", "IT"));

        bisq.account.accounts.SepaInstantAccountPayload payload2 = new bisq.account.accounts.SepaInstantAccountPayload(
                "id",
                "SEPA_INSTANT",
                "holderName",
                "DE89370400440532013000",
                "DEUTDEBBXXX",
                "DE",
                List.of("DE", "FR", "IT"));

        assertEquals(payload1, payload2);
        assertEquals(payload1.hashCode(), payload2.hashCode());

        bisq.account.accounts.SepaInstantAccountPayload differentPayload = new bisq.account.accounts.SepaInstantAccountPayload(
                "id",
                "SEPA_INSTANT",
                "differentHolder",
                "DE89370400440532013000",
                "DEUTDEBBXXX",
                "DE",
                List.of("DE", "FR", "IT"));

        assertNotEquals(payload1, differentPayload);
    }

    @Test
    void testNullHolderName() {
        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaInstantAccountPayload(
                        "id",
                        "SEPA_INSTANT",
                        null,  // null holder name
                        "DE89370400440532013000",
                        "DEUTDEBBXXX",
                        "DE",
                        List.of("DE", "FR", "IT"))
        );
    }

    @Test
    void testEmptyHolderName() {
        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaInstantAccountPayload(
                        "id",
                        "SEPA_INSTANT",
                        "",  // empty holder name
                        "DE89370400440532013000",
                        "DEUTDEBBXXX",
                        "DE",
                        List.of("DE", "FR", "IT"))
        );
    }

    @Test
    void testBlankHolderName() {
        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaInstantAccountPayload(
                        "id",
                        "SEPA_INSTANT",
                        "   ",  // blank holder name (only whitespace)
                        "DE89370400440532013000",
                        "DEUTDEBBXXX",
                        "DE",
                        List.of("DE", "FR", "IT"))
        );
    }

    @Test
    void testNullCountryCodeInList() {
        List<String> listWithNull = new ArrayList<>();
        listWithNull.add("DE");
        listWithNull.add(null);
        listWithNull.add("FR");

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaInstantAccountPayload(
                        "id",
                        "SEPA_INSTANT",
                        "holderName",
                        "DE89370400440532013000",
                        "DEUTDEBBXXX",
                        "DE",
                        listWithNull)
        );
    }

    @Test
    void testEmptyCountryCodeInList() {
        List<String> listWithEmpty = List.of("DE", "", "FR");

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaInstantAccountPayload(
                        "id",
                        "SEPA_INSTANT",
                        "holderName",
                        "DE89370400440532013000",
                        "DEUTDEBBXXX",
                        "DE",
                        listWithEmpty)
        );
    }

    @Test
    void testBlankCountryCodeInList() {
        List<String> listWithBlank = List.of("DE", "   ", "FR");

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SepaInstantAccountPayload(
                        "id",
                        "SEPA_INSTANT",
                        "holderName",
                        "DE89370400440532013000",
                        "DEUTDEBBXXX",
                        "DE",
                        listWithBlank)
        );
    }
}