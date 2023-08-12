package bisq.account.accounts;

import bisq.account.protobuf.Account;
import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.FiatPaymentMethod;
import bisq.account.protobuf.InteracETransferAccount;
import bisq.account.protobuf.InteracETransferAccountPayload;
import bisq.account.protobuf.PaymentMethod;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InteracETransferAccountTest {

    private static final bisq.account.protobuf.Account PROTO = Account.newBuilder()
            .setAccountName("accountName")
            .setCreationDate(123)
            .setPaymentMethod(
                    PaymentMethod.newBuilder()
                            .setName("INTERAC_E_TRANSFER")
                            .setFiatPaymentMethod(
                                    FiatPaymentMethod.newBuilder()))
            .setAccountPayload(AccountPayload.newBuilder()
                    .setId("id")
                    .setPaymentMethodName("INTERAC_E_TRANSFER")
                    .setInteracETransferAccountPayload(InteracETransferAccountPayload.newBuilder()
                            .setEmail("email")
                            .setHolderName("holderName")
                            .setQuestion("question")
                            .setAnswer("answer")))
            .setInteracETransferAccount(InteracETransferAccount.newBuilder())
            .build();

    private static final bisq.account.accounts.InteracETransferAccount ACCOUNT = new bisq.account.accounts.InteracETransferAccount(
            123,
            "accountName",
            new bisq.account.accounts.InteracETransferAccountPayload(
                    "id", "INTERAC_E_TRANSFER", "email", "holderName", "question", "answer")
    );

    @Test
    void toProto() {
        var result = ACCOUNT.toProto();
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(PROTO);
    }

    @Test
    void fromProto() {
        var result = bisq.account.accounts.InteracETransferAccount.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(ACCOUNT);
    }
}