package bisq.account.accounts;

import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.AchTransferAccountPayload;
import bisq.account.protobuf.BankAccountPayload;
import bisq.account.protobuf.CountryBasedAccountPayload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AchTransferAccountPayloadTest {

    private static final AccountPayload PROTO = bisq.account.protobuf.AccountPayload.newBuilder()
            .setId("id")
            .setPaymentMethodName("paymentMethodName")
            .setCountryBasedAccountPayload(CountryBasedAccountPayload.newBuilder()
                    .setCountryCode("countryCode")
                    .setBankAccountPayload(BankAccountPayload.newBuilder()
                            .setHolderName("holderName")
                            .setAccountNr("accountNr")
                            .setAccountType("accountType")
                            .setBankName("bankName")
                            .setBranchId("branchId")
                            .setAchTransferAccountPayload(AchTransferAccountPayload.newBuilder()
                                    .setHolderAddress("holderAddress"))
                    )
            ).build();

    private static final AccountPayload PROTO_OPTIONALS_NOT_SET = bisq.account.protobuf.AccountPayload.newBuilder()
            .setId("id")
            .setPaymentMethodName("paymentMethodName")
            .setCountryBasedAccountPayload(CountryBasedAccountPayload.newBuilder()
                    .setCountryCode("countryCode")
                    .setBankAccountPayload(BankAccountPayload.newBuilder()
                            .setHolderName("holderName")
                            .setAchTransferAccountPayload(AchTransferAccountPayload.newBuilder())))
            .build();

    private static final bisq.account.accounts.AchTransferAccountPayload PAYLOAD =
            new bisq.account.accounts.AchTransferAccountPayload(
                    "id", "paymentMethodName", "countryCode", "holderName", "bankName", "branchId", "accountNr", "accountType", "holderAddress"
            );
    private static final bisq.account.accounts.AchTransferAccountPayload PAYLOAD_OPTIONALS_NOT_SET =
            new bisq.account.accounts.AchTransferAccountPayload(
                    "id", "paymentMethodName", "countryCode", "holderName", null, null, null, null, null
            );

    @Test
    void toProto() {
        assertEquals(PROTO, PAYLOAD.toProto());
    }

    @Test
    void fromProto() {
        assertEquals(PAYLOAD, bisq.account.accounts.AchTransferAccountPayload.fromProto(PROTO));
    }

    @Test
    void fromProto_optionals() {
        assertEquals(PAYLOAD_OPTIONALS_NOT_SET, bisq.account.accounts.AchTransferAccountPayload.fromProto(PROTO_OPTIONALS_NOT_SET));
    }
}