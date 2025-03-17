package bisq.account.accounts;

import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.BankAccountPayload;
import bisq.account.protobuf.CountryBasedAccountPayload;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NationalBankAccountPayloadTest {

    private static final bisq.account.protobuf.AccountPayload PROTO = bisq.account.protobuf.AccountPayload.newBuilder()
            .setId("id")
            .setPaymentMethodName("paymentMethodName")
            .setCountryBasedAccountPayload(bisq.account.protobuf.CountryBasedAccountPayload.newBuilder()
                    .setCountryCode("countryCode")
                    .setBankAccountPayload(bisq.account.protobuf.BankAccountPayload.newBuilder()
                            .setHolderName("holderName")
                            .setAccountNr("accountNr")
                            .setAccountType("accountType")
                            .setBankName("bankName")
                            .setBranchId("branchId")
                            .setHolderTaxId("holderTaxId")
                            .setBankId("bankId")
                            .setNationalAccountId("nationalAccountId")
                            .setNationalBankAccountPayload(bisq.account.protobuf.NationalBankAccountPayload.newBuilder()))
            ).build();

    private static final AccountPayload PROTO_OPTIONALS_NOT_SET = bisq.account.protobuf.AccountPayload.newBuilder()
            .setId("id")
            .setPaymentMethodName("paymentMethodName")
            .setCountryBasedAccountPayload(CountryBasedAccountPayload.newBuilder()
                    .setCountryCode("countryCode")
                    .setBankAccountPayload(BankAccountPayload.newBuilder()
                            .setHolderName("holderName")
                            .setNationalBankAccountPayload(bisq.account.protobuf.NationalBankAccountPayload.newBuilder())))
            .build();

    private static final bisq.account.accounts.NationalBankAccountPayload PAYLOAD =
            new bisq.account.accounts.NationalBankAccountPayload(
                    "id", "paymentMethodName", "countryCode",
                    Optional.of("holderName"), Optional.of("bankName"), Optional.of("branchId"),
                    Optional.of("accountNr"), Optional.of("accountType"), Optional.of("holderTaxId"),
                    Optional.of("bankId"), Optional.of("nationalAccountId")
            );
    private static final bisq.account.accounts.NationalBankAccountPayload PAYLOAD_OPTIONALS_NOT_SET =
            new bisq.account.accounts.NationalBankAccountPayload(
                    "id", "paymentMethodName", "countryCode",
                    Optional.of("holderName"), null, null, null, null,
                    null, null, null);

    @Test
    void testToProto() {
        assertEquals(PROTO, PAYLOAD.completeProto());
    }

    @Test
    void testFromProto() {
        assertEquals(PAYLOAD, bisq.account.accounts.NationalBankAccountPayload.fromProto(PROTO));
    }

    @Test
    void fromProto_optionals() {
        assertEquals(PAYLOAD_OPTIONALS_NOT_SET, bisq.account.accounts.NationalBankAccountPayload.fromProto(PROTO_OPTIONALS_NOT_SET));
    }
}