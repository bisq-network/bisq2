package bisq.api.dto.mappings.account;

import bisq.account.accounts.Account;
import bisq.account.accounts.crypto.MoneroAccount;
import bisq.account.accounts.crypto.OtherCryptoAssetAccount;
import bisq.account.accounts.fiat.AchTransferAccount;
import bisq.account.accounts.fiat.AliPayAccount;
import bisq.account.accounts.fiat.AmazonGiftCardAccount;
import bisq.account.accounts.fiat.MoneyGramAccount;
import bisq.account.accounts.fiat.UserDefinedFiatAccount;
import bisq.account.accounts.fiat.WiseAccount;
import bisq.account.accounts.fiat.ZelleAccount;
import bisq.account.payment_method.PaymentMethod;
import bisq.api.dto.account.PaymentAccountDto;
import bisq.api.dto.mappings.account.crypto.MoneroAccountDtoMapping;
import bisq.api.dto.mappings.account.crypto.OtherCryptoAssetAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.AchTransferAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.AliPayAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.AmazonGiftCardAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.MoneyGramAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.UserDefinedFiatAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.WiseAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.ZelleAccountDtoMapping;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class PaymentAccountDtoMapping {
    public static Optional<PaymentAccountDto>   fromBisq2ModelIfSupported(Account<? extends PaymentMethod<?>, ?> account) {
        try {
            return Optional.of(fromBisq2Model(account));
        } catch (RuntimeException e) {
            String accountType = account == null ? "null" : account.getClass().getSimpleName();
            String accountName = account == null ? "null" : account.getAccountName();
            log.warn("Skipping payment account that could not be converted. accountName={}, accountType={}", accountName, accountType, e);
            return Optional.empty();
        }
    }

    /**
     * Convert a Bisq2 Account model to a PaymentAccountDto.
     * Currently supports:
     * - Fiat: UserDefinedFiatAccount, AchTransferAccount, AliPayAccount, AmazonGiftCardAccount, MoneyGramAccount, WiseAccount, ZelleAccount
     * - Crypto: MoneroAccount, OtherCryptoAssetAccount
     * TODO: Add support for additional account types as they are implemented.
     */
    public static PaymentAccountDto fromBisq2Model(Account<? extends PaymentMethod<?>, ?> account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }

        if (account instanceof UserDefinedFiatAccount userDefinedAccount) {
            return UserDefinedFiatAccountDtoMapping.fromBisq2Model(userDefinedAccount);
        }
        if (account instanceof AchTransferAccount achTransferAccount) {
            return AchTransferAccountDtoMapping.fromBisq2Model(achTransferAccount);
        }
        if (account instanceof AliPayAccount aliPayAccount) {
            return AliPayAccountDtoMapping.fromBisq2Model(aliPayAccount);
        }
        if (account instanceof AmazonGiftCardAccount amazonGiftCardAccount) {
            return AmazonGiftCardAccountDtoMapping.fromBisq2Model(amazonGiftCardAccount);
        }
        if (account instanceof MoneyGramAccount moneyGramAccount) {
            return MoneyGramAccountDtoMapping.fromBisq2Model(moneyGramAccount);
        }
        if (account instanceof WiseAccount wiseAccount) {
            return WiseAccountDtoMapping.fromBisq2Model(wiseAccount);
        }
        if (account instanceof ZelleAccount zelleAccount) {
            return ZelleAccountDtoMapping.fromBisq2Model(zelleAccount);
        }
        if (account instanceof MoneroAccount moneroAccount) {
            return MoneroAccountDtoMapping.fromBisq2Model(moneroAccount);
        }
        if (account instanceof OtherCryptoAssetAccount otherCryptoAssetAccount) {
            return OtherCryptoAssetAccountDtoMapping.fromBisq2Model(otherCryptoAssetAccount);
        }

        // TODO: Add conversions for other account types when implemented:
        // if (account instanceof SepaAccount sepaAccount) { ... }
        // if (account instanceof RevolutAccount revolutAccount) { ... }

        throw new IllegalArgumentException("Unsupported account type: " + account.getClass().getSimpleName());
    }
}
