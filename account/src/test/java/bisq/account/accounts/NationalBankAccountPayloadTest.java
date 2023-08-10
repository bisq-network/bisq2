package bisq.account.accounts;

import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.BankAccountPayload;
import bisq.account.protobuf.CountryBasedAccountPayload;
import org.junit.jupiter.api.Test;

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
                    "holderName", "bankName", "branchId",
                    "accountNr", "accountType", "holderTaxId",
                    "bankId", "nationalAccountId"
            );
    private static final bisq.account.accounts.NationalBankAccountPayload PAYLOAD_OPTIONALS_NOT_SET =
            new bisq.account.accounts.NationalBankAccountPayload(
                    "id", "paymentMethodName", "countryCode",
                    "holderName", null, null,
                    null, null, null,
                    null, null);

    @Test
    void toProto() {
        assertEquals(PROTO, PAYLOAD.toProto());
    }

    @Test
    void fromProto() {
        assertEquals(PAYLOAD, bisq.account.accounts.NationalBankAccountPayload.fromProto(PROTO));
    }

    @Test
    void fromProto_optionals() {
        assertEquals(PAYLOAD_OPTIONALS_NOT_SET, bisq.account.accounts.NationalBankAccountPayload.fromProto(PROTO_OPTIONALS_NOT_SET));
    }
}