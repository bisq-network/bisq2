package bisq.account.accounts;

import bisq.account.protobuf.Account;
import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.AmazonGiftCardAccount;
import bisq.account.protobuf.AmazonGiftCardAccountPayload;
import bisq.account.protobuf.CountryBasedAccount;
import bisq.account.protobuf.CountryBasedAccountPayload;
import bisq.account.protobuf.*;
import bisq.common.protobuf.Country;
import bisq.common.protobuf.Region;
import org.junit.jupiter.api.Test;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class AmazonGiftCardAccountTest {

    private static final Account PROTO = Account.newBuilder()
            .setAccountName("accountName")
            .setCreationDate(123)
            .setPaymentMethod(PaymentMethod.newBuilder()
                    .setName("AMAZON_GIFT_CARD")
                    .setFiatPaymentMethod(FiatPaymentMethod.newBuilder().build())
                    .build())
            .setAccountPayload(AccountPayload.newBuilder()
                    .setId("id")
                    .setPaymentRailName("AMAZON_GIFT_CARD")
                    .setCountryBasedAccountPayload(CountryBasedAccountPayload.newBuilder()
                            .setCountryCode("US")
                            .setAmazonGiftCardAccountPayload(AmazonGiftCardAccountPayload.newBuilder()
                                    .setEmailOrMobileNr("emailOrMobileNr")
                            ))
            )
            .setCountryBasedAccount(CountryBasedAccount.newBuilder()
                    .setAmazonGiftCardAccount(AmazonGiftCardAccount.newBuilder())
            )
            .build();

    private static final bisq.account.accounts.AmazonGiftCardAccount ACCOUNT =
            new bisq.account.accounts.AmazonGiftCardAccount(
                    "accountName",
                    new bisq.account.accounts.AmazonGiftCardAccountPayload("id", "AMAZON_GIFT_CARD", "US", "emailOrMobileNr"));

    @Test
    void testToProto() {
        var result = ACCOUNT.completeProto();
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("accountPayload_.memoizedHashCode", "memoizedHashCode", "creationDate_")
                .isEqualTo(PROTO);
        assertThat(result.getCreationDate()).isCloseTo(currentTimeMillis(), offset(1000L));
    }

    @Test
    void testFromProto() {
        var result = bisq.account.accounts.AmazonGiftCardAccount.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("creationDate")
                .isEqualTo(ACCOUNT);
        assertThat(result.getCreationDate()).isCloseTo(System.currentTimeMillis(), offset(1000L));
    }
}