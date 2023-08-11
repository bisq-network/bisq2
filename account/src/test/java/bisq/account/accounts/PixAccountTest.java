package bisq.account.accounts;

import bisq.account.protobuf.Account;
import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.CountryBasedAccount;
import bisq.account.protobuf.CountryBasedAccountPayload;
import bisq.account.protobuf.FiatPaymentMethod;
import bisq.account.protobuf.PaymentMethod;
import bisq.common.protobuf.Country;
import bisq.common.protobuf.Region;
import org.junit.jupiter.api.Test;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class PixAccountTest {

    private static final Account PROTO = Account.newBuilder()
            .setAccountName("accountName")
            .setCreationDate(123)
            .setPaymentMethod(PaymentMethod.newBuilder()
                    .setName("PIX")
                    .setFiatPaymentMethod(FiatPaymentMethod.newBuilder().build())
                    .build())
            .setAccountPayload(AccountPayload.newBuilder()
                    .setId("id")
                    .setPaymentMethodName("PIX")
                    .setCountryBasedAccountPayload(CountryBasedAccountPayload.newBuilder()
                            .setCountryCode("countryCode")
                            .setPixAccountPayload(bisq.account.protobuf.PixAccountPayload.newBuilder()
                                    .setPixKey("pixKey")))
            )
            .setCountryBasedAccount(CountryBasedAccount.newBuilder()
                    .setCountry(Country.newBuilder()
                            .setCode("countryCode")
                            .setName("countryName")
                            .setRegion(Region.newBuilder()
                                    .setCode("regionCode")
                                    .setName("regionName")))
                    .setPixAccount(bisq.account.protobuf.PixAccount.newBuilder().build())
            )
            .build();

    private static final bisq.account.accounts.PixAccount ACCOUNT = new bisq.account.accounts.PixAccount(
            "accountName",
            new bisq.account.accounts.PixAccountPayload("id", "PIX", "countryCode", "pixKey"),
            new bisq.common.locale.Country(
                    "countryCode",
                    "countryName",
                    new bisq.common.locale.Region("regionCode", "regionName")));

    @Test
    void toProto() {
        var result = ACCOUNT.toProto();
        assertThat(result).usingRecursiveComparison()
                .ignoringFields("accountPayload_.memoizedHashCode", "memoizedHashCode", "creationDate_")
                .isEqualTo(PROTO);
        assertThat(result.getCreationDate()).isCloseTo(currentTimeMillis(), offset(1000L));
    }

    @Test
    void fromProto() {
        var result = bisq.account.accounts.PixAccount.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("creationDate")
                .isEqualTo(ACCOUNT);
        assertThat(result.getCreationDate()).isCloseTo(System.currentTimeMillis(), offset(1000L));
    }
}