package bisq.account.accounts;

import bisq.account.accounts.crypto.MoneroAccountPayload;
import bisq.account.accounts.crypto.OtherCryptoAssetAccountPayload;
import bisq.account.accounts.fiat.AchTransferAccountPayload;
import bisq.account.accounts.fiat.AdvancedCashAccountPayload;
import bisq.account.accounts.fiat.AliPayAccountPayload;
import bisq.account.accounts.fiat.AmazonGiftCardAccountPayload;
import bisq.account.accounts.fiat.BankAccountPayload;
import bisq.account.accounts.fiat.BankAccountType;
import bisq.account.accounts.fiat.BizumAccountPayload;
import bisq.account.accounts.fiat.CashByMailAccountPayload;
import bisq.account.accounts.fiat.CashDepositAccountPayload;
import bisq.account.accounts.fiat.DomesticWireTransferAccountPayload;
import bisq.account.accounts.fiat.F2FAccountPayload;
import bisq.account.accounts.fiat.FasterPaymentsAccountPayload;
import bisq.account.accounts.fiat.HalCashAccountPayload;
import bisq.account.accounts.fiat.ImpsAccountPayload;
import bisq.account.accounts.fiat.InteracETransferAccountPayload;
import bisq.account.accounts.fiat.MercadoPagoAccountPayload;
import bisq.account.accounts.fiat.MoneseAccountPayload;
import bisq.account.accounts.fiat.MoneyBeamAccountPayload;
import bisq.account.accounts.fiat.MoneyGramAccountPayload;
import bisq.account.accounts.fiat.NationalBankAccountPayload;
import bisq.account.accounts.fiat.NeftAccountPayload;
import bisq.account.accounts.fiat.PayIdAccountPayload;
import bisq.account.accounts.fiat.PayseraAccountPayload;
import bisq.account.accounts.fiat.PerfectMoneyAccountPayload;
import bisq.account.accounts.fiat.PixAccountPayload;
import bisq.account.accounts.fiat.PromptPayAccountPayload;
import bisq.account.accounts.fiat.RevolutAccountPayload;
import bisq.account.accounts.fiat.SameBankAccountPayload;
import bisq.account.accounts.fiat.SatispayAccountPayload;
import bisq.account.accounts.fiat.SbpAccountPayload;
import bisq.account.accounts.fiat.SepaAccountPayload;
import bisq.account.accounts.fiat.SepaInstantAccountPayload;
import bisq.account.accounts.fiat.StrikeAccountPayload;
import bisq.account.accounts.fiat.SwishAccountPayload;
import bisq.account.accounts.fiat.USPostalMoneyOrderAccountPayload;
import bisq.account.accounts.fiat.UpholdAccountPayload;
import bisq.account.accounts.fiat.UpiAccountPayload;
import bisq.account.accounts.fiat.UserDefinedFiatAccountPayload;
import bisq.account.accounts.fiat.WeChatPayAccountPayload;
import bisq.account.accounts.fiat.WiseAccountPayload;
import bisq.account.accounts.fiat.WiseUsdAccountPayload;
import bisq.account.accounts.fiat.ZelleAccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.util.ByteArrayUtils;
import bisq.i18n.Res;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@Slf4j
public class AccountPayloadBisq1CompatibleFingerprintTest {
    private static final byte[] SALT = new byte[32];

    @BeforeAll
    static void setUp() {
        Res.setAndApplyLanguageTag("en");
    }

    @Test
    void zelleFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: ClearXchange uses CLEAR_X_CHANGE without country code.
        ZelleAccountPayload payload = new ZelleAccountPayload("id", SALT, "Alice", "alice@example.com");
        assertArrayEquals(expected("CLEAR_X_CHANGE", "alice@example.com"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void usPostalMoneyOrderFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: USPostalMoneyOrder uses holderName and postalAddress without country code.
        USPostalMoneyOrderAccountPayload payload = new USPostalMoneyOrderAccountPayload("id", SALT, "Alice", "123 Main St");
        assertArrayEquals(expected("US_POSTAL_MONEY_ORDER", "Alice", "123 Main St"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void payIdFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: Australia PayID uses AUSTRALIA_PAYID without country code.
        PayIdAccountPayload payload = new PayIdAccountPayload("id", SALT, "Alice", "alice$bank");
        assertArrayEquals(expected("AUSTRALIA_PAYID", "alice$bank", "Alice"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void interacETransferFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: Interac uses email, question, answer without country code.
        InteracETransferAccountPayload payload = new InteracETransferAccountPayload(
                "id", SALT, "Alice", "alice@example.com", "Question", "Answer");
        assertArrayEquals(expected("INTERAC_E_TRANSFER", "alice@example.com", "Question", "Answer"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void halCashFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: HalCash uses mobile number without country code.
        HalCashAccountPayload payload = new HalCashAccountPayload("id", SALT, "+34612345678");
        assertArrayEquals(expected("HAL_CASH", "+34612345678"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void fasterPaymentsFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: Faster Payments uses sort code and account number without country code.
        FasterPaymentsAccountPayload payload = new FasterPaymentsAccountPayload("id", SALT, "Alice", "123456", "12345678");
        assertArrayEquals(expected("FASTER_PAYMENTS", "123456", "12345678"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void sbpFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: SBP uses mobile number and bank name without country code.
        SbpAccountPayload payload = new SbpAccountPayload("id", SALT, "Alice", "+79161234567", "Sberbank");
        assertArrayEquals(expected("SBP", "+79161234567", "Sberbank"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void aliPayFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: AliPay uses account number without country code.
        AliPayAccountPayload payload = new AliPayAccountPayload("id", SALT, "alipay-123");
        assertArrayEquals(expected("ALI_PAY", "alipay-123"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void weChatPayFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: WeChat Pay uses account number without country code.
        WeChatPayAccountPayload payload = new WeChatPayAccountPayload("id", SALT, "wechat-456");
        assertArrayEquals(expected("WECHAT_PAY", "wechat-456"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void advancedCashFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: Advanced Cash uses account number.
        AdvancedCashAccountPayload payload = new AdvancedCashAccountPayload("id", SALT, List.of("USD"), "A123456789012");
        assertArrayEquals(expected("ADVANCED_CASH", "A123456789012"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void perfectMoneyFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: Perfect Money uses account number.
        PerfectMoneyAccountPayload payload = new PerfectMoneyAccountPayload("id", SALT, "USD", "U1234567");
        assertArrayEquals(expected("PERFECT_MONEY", "U1234567"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void moneseFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: Monese uses holder name.
        MoneseAccountPayload payload = new MoneseAccountPayload("id", SALT, List.of("EUR"), "Alice", "+49123456789");
        assertArrayEquals(expected("MONESE", "Alice"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void payseraFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: Paysera uses email.
        PayseraAccountPayload payload = new PayseraAccountPayload("id", SALT, List.of("EUR"), "alice@example.com");
        assertArrayEquals(expected("PAYSERA", "alice@example.com"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void satispayFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: Satispay includes country code and holder name.
        SatispayAccountPayload payload = new SatispayAccountPayload("id", SALT, "Alice", "+393331234567");
        assertArrayEquals(expected("SATISPAY", "IT", "Alice"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void mercadoPagoFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: MercadoPago includes country code, holder id, and holder name.
        MercadoPagoAccountPayload payload = new MercadoPagoAccountPayload("id", SALT, "Alice", "DNI1234");
        assertArrayEquals(expected("MERCADO_PAGO", "AR", "DNI1234", "Alice"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void impsFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: IMPS includes country code and account number.
        ImpsAccountPayload payload = new ImpsAccountPayload("id", SALT, "Alice", "123456", "IFSC1234");
        assertArrayEquals(expected("IMPS", "IN", "123456"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void neftFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: NEFT includes country code and account number.
        NeftAccountPayload payload = new NeftAccountPayload("id", SALT, "Alice", "123456", "IFSC1234");
        assertArrayEquals(expected("NEFT", "IN", "123456"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void amazonGiftCardFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: AmazonGiftCard uses the "AmazonGiftCard" prefix without country code.
        AmazonGiftCardAccountPayload payload = new AmazonGiftCardAccountPayload("id", SALT, "US", "USD", "alice@example.com");
        assertArrayEquals(expected("AMAZON_GIFT_CARD", "AmazonGiftCard", "alice@example.com"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void swishFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: Swish uses mobile number without country code.
        SwishAccountPayload payload = new SwishAccountPayload("id", SALT, "SE", "Alice", "+46701234567");
        assertArrayEquals(expected("SWISH", "+46701234567"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void cashByMailFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: CashByMail uses contact and postal address.
        CashByMailAccountPayload payload = new CashByMailAccountPayload("id", SALT, "USD", "123 Main St", "Alice", "Extra");
        assertArrayEquals(expected("CASH_BY_MAIL", "Alice", "123 Main St"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void sepaFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: SEPA includes country code, IBAN, and BIC.
        SepaAccountPayload payload = new SepaAccountPayload(
                "id", SALT, "Alice", "DE89370400440532013000", "DEUTDEFF", "DE", List.of("DE"));
        assertArrayEquals(expected("SEPA", "DE", "DE89370400440532013000", "DEUTDEFF"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void sepaInstantFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: SEPA Instant includes country code, IBAN, and BIC.
        SepaInstantAccountPayload payload = new SepaInstantAccountPayload(
                "id", SALT, "Alice", "DE89370400440532013000", "DEUTDEFF", "DE", List.of("DE"));
        assertArrayEquals(expected("SEPA_INSTANT", "DE", "DE89370400440532013000", "DEUTDEFF"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void bizumFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: Bizum includes country code and mobile number.
        BizumAccountPayload payload = new BizumAccountPayload("id", SALT, "ES", "+34612345678");
        assertArrayEquals(expected("BIZUM", "ES", "+34612345678"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void f2fFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: F2F includes country code, contact, and city.
        F2FAccountPayload payload = new F2FAccountPayload("id", SALT, "US", "USD", "New York", "Alice", "Extra");
        assertArrayEquals(expected("F2F", "US", "Alice", "New York"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void pixFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: Pix includes country code, pix key, and holder name.
        PixAccountPayload payload = new PixAccountPayload("id", SALT, "BR", "Alice", "pix-key");
        assertArrayEquals(expected("PIX", "BR", "pix-key", "Alice"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void promptPayFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: PromptPay uses promptPayId without country code.
        PromptPayAccountPayload payload = new PromptPayAccountPayload("id", SALT, "TH", "1234567890");
        assertArrayEquals(expected("PROMPT_PAY", "1234567890"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void strikeFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: Strike includes country code and holder name.
        StrikeAccountPayload payload = new StrikeAccountPayload("id", SALT, "US", "Alice");
        assertArrayEquals(expected("STRIKE", "US", "Alice"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void upiFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: UPI includes country code and virtual payment address.
        UpiAccountPayload payload = new UpiAccountPayload("id", SALT, "IN", "alice@upi");
        assertArrayEquals(expected("UPI", "IN", "alice@upi"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void wiseFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: TransferWise uses email + holder name without country code.
        WiseAccountPayload payload = new WiseAccountPayload("id", SALT, List.of("EUR"), "Alice", "alice@example.com");
        assertArrayEquals(expected("TRANSFERWISE", "alice@example.com", "Alice"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void wiseUsdFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: TransferWise USD includes country code and holder name.
        WiseUsdAccountPayload payload = new WiseUsdAccountPayload(
                "id", SALT, "US", "Alice", "alice@example.com", "123 Main St");
        assertArrayEquals(expected("TRANSFERWISE_USD", "US", "Alice"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void moneyGramFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: MoneyGram includes country code, state, holder name, and email.
        MoneyGramAccountPayload payload = new MoneyGramAccountPayload(
                "id", SALT, "US", List.of("USD"), "Alice", "alice@example.com", "CA");
        assertArrayEquals(expected("MONEY_GRAM", "US", "CA", "Alice", "alice@example.com"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void moneyBeamFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: MoneyBeam uses account id and holder name without country code.
        MoneyBeamAccountPayload payload = new MoneyBeamAccountPayload(
                "id", SALT, "DE", "Alice", "alice@example.com");
        assertArrayEquals(expected("MONEY_BEAM", "alice@example.com", "Alice"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void revolutFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: Revolut uses user name.
        RevolutAccountPayload payload = new RevolutAccountPayload("id", SALT, "alice", List.of("EUR"));
        assertArrayEquals(expected("REVOLUT", "alice"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void upholdFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: Uphold uses account id.
        UpholdAccountPayload payload = new UpholdAccountPayload("id", SALT, List.of("USD"), "Alice", "acct-1");
        assertArrayEquals(expected("UPHOLD", "acct-1"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void achTransferFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: ACH uses bank account fields with country prefix.
        AchTransferAccountPayload payload = new AchTransferAccountPayload(
                "id", SALT, "Alice", "123 Main St", "Bank", "111000025", "123456789", BankAccountType.CHECKING);
        assertArrayEquals(expected("ACH_TRANSFER", "US", "Bank", "", "null", "123456789", "Checking", "", ""),
                payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void cashDepositFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: Cash Deposit uses bank account fields with country prefix.
        CashDepositAccountPayload payload = new CashDepositAccountPayload(
                "id", SALT, "US", "USD", "Alice", Optional.empty(), "Bank", Optional.of("111000025"),
                Optional.empty(), "123456789", Optional.of(BankAccountType.CHECKING), Optional.empty(), Optional.empty());
        assertArrayEquals(expected("CASH_DEPOSIT", "US", "Bank", "", "null", "123456789", "Checking", "", ""),
                payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void domesticWireTransferFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: Domestic wire uses bank account fields with country prefix.
        DomesticWireTransferAccountPayload payload = new DomesticWireTransferAccountPayload(
                "id", SALT, "Alice", "123 Main St", "Bank", "111000025", "123456789");
        assertArrayEquals(expected("DOMESTIC_WIRE_TRANSFER", "US", "Bank", "", "null", "123456789", "", "", ""),
                payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void nationalBankFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: National bank uses bank account fields with country prefix.
        NationalBankAccountPayload payload = new NationalBankAccountPayload(
                "id", SALT, "SE", "SEK", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), "1234567", Optional.empty(), Optional.empty());
        assertArrayEquals(expected("NATIONAL_BANK", "SE", "1234567"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void sameBankFingerprintMatchesBisq1() {
        // Bisq 1 compatibility: Same bank uses bank account fields with country prefix.
        SameBankAccountPayload payload = new SameBankAccountPayload(
                "id", SALT, "SE", "SEK", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), "1234567", Optional.empty(), Optional.empty());
        assertArrayEquals(expected("SAME_BANK", "SE", "1234567"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void bankAccountFingerprintIncludesRequiredFields() {
        // Bisq 1 compatibility: BankAccountPayload uses country prefix and required fields.
        BankAccountPayload payload = new TestBankAccountPayload(
                "id", "US", "USD",
                Optional.of("Alice"), Optional.of("ID123"), Optional.of("Bank"),
                Optional.of("111000025"), Optional.of("001"), "123456789",
                Optional.of(BankAccountType.CHECKING), Optional.of("NAT"));
        payload.verify();

        assertArrayEquals(expected("NATIONAL_BANK", "US", "Bank", "", "001", "123456789", "Checking", "", ""),
                payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void bankAccountFingerprintIgnoresOptionalFieldsWhenNotRequired() {
        // Bisq 1 compatibility: optional fields are excluded when not required by country.
        BankAccountPayload payload = new TestBankAccountPayload(
                "id", "SE", "SEK",
                Optional.of("Alice"), Optional.of("ID123"), Optional.of("Bank"),
                Optional.of("BANKID"), Optional.of("BRANCH"), "1234567",
                Optional.of(BankAccountType.SAVINGS), Optional.of("NAT"));
        payload.verify();

        assertArrayEquals(expected("NATIONAL_BANK", "SE", "Bank", "1234567"),
                payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void bankAccountFingerprintIncludesHolderIdAndNationalAccountId() {
        // Bisq 1 compatibility: holderId and nationalAccountId are included when required.
        // Need to consider the isRequired methods in BankAccountUtils (e.g. isBankNameRequired) for the given country.
        BankAccountPayload payload = new TestBankAccountPayload(
                "id",
                "AR",
                "ARS",
                Optional.of("Alice"),
                Optional.of("CUIT123"),
                Optional.of("BankXYZ"),
                Optional.empty(),
                Optional.of("BRANCH"), "1234567",
                Optional.empty(),
                Optional.of("CBU123"));
        payload.verify();
        assertArrayEquals(expected("NATIONAL_BANK", "AR", "BankXYZ", "", "BRANCH", "1234567", "",
                        "CUIL/CUIT CUIT123\n", "CBU123"),
                payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void userDefinedFiatFingerprint() {
        UserDefinedFiatAccountPayload payload = new UserDefinedFiatAccountPayload("id", "custom data");
        assertArrayEquals(expected("CUSTOM", "custom data"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void moneroFingerprintIncludesPrivateViewKey() {
        MoneroAccountPayload payload = new MoneroAccountPayload(
                "id", SALT, "XMR", "address", false, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), false, Optional.empty(), Optional.of("viewkey"), Optional.empty(),
                Optional.empty(), Optional.empty());
        assertArrayEquals(expected("NATIVE_CHAIN", "XMR", "address", "viewkey"), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void moneroFingerprintOmitsPrivateViewKeyWhenMissing() {
        MoneroAccountPayload payload = new MoneroAccountPayload(
                "id", SALT, "XMR", "address", false, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), false, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
        assertArrayEquals(expected("NATIVE_CHAIN", "XMR", "address", ""), payload.getBisq1CompatibleFingerprint());
    }

    @Test
    void otherCryptoAssetFingerprint() {
        OtherCryptoAssetAccountPayload payload = new OtherCryptoAssetAccountPayload(
                "id", SALT, "DOGE", "D123", false, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty());
        assertArrayEquals(expected("NATIVE_CHAIN", "DOGE", "D123"), payload.getBisq1CompatibleFingerprint());
    }

    private static byte[] expected(String paymentMethodId, String... dataParts) {
        return ByteArrayUtils.concat(bytes(paymentMethodId), concatStrings(dataParts));
    }

    private static byte[] concatStrings(String... parts) {
        byte[][] arrays = new byte[parts.length][];
        for (int i = 0; i < parts.length; i++) {
            arrays[i] = bytes(parts[i]);
        }
        return ByteArrayUtils.concat(arrays);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static final class TestBankAccountPayload extends BankAccountPayload {
        private TestBankAccountPayload(String id,
                                       String countryCode,
                                       String selectedCurrencyCode,
                                       Optional<String> holderName,
                                       Optional<String> holderId,
                                       Optional<String> bankName,
                                       Optional<String> bankId,
                                       Optional<String> branchId,
                                       String accountNr,
                                       Optional<BankAccountType> bankAccountType,
                                       Optional<String> nationalAccountId) {
            super(id,
                    SALT,
                    countryCode,
                    selectedCurrencyCode,
                    holderName,
                    holderId,
                    bankName,
                    bankId,
                    branchId,
                    accountNr,
                    bankAccountType,
                    nationalAccountId);
        }

        @Override
        public FiatPaymentMethod getPaymentMethod() {
            return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);
        }
    }
}
